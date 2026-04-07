package com.assistencia.controller;

import com.assistencia.model.OrdemServico;
import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.model.Venda;
import com.assistencia.model.Empresa;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.EmpresaRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClienteRepository clienteRepo;
    private final OrdemServicoRepository ordemRepo;
    private final VendaRepository vendaRepo;
    private final UsuarioRepository usuarioRepo;
    private final PagamentoComissaoRepository pagamentoRepo;
    private final EmpresaRepository empresaRepo;

    @Value("${mercado_pago_sample_access_token}")
    private String mpAccessToken;

    public AdminController(ClienteRepository clienteRepo, OrdemServicoRepository ordemRepo,
                           VendaRepository vendaRepo, UsuarioRepository usuarioRepo,
                           PagamentoComissaoRepository pagamentoRepo, EmpresaRepository empresaRepo) {
        this.clienteRepo = clienteRepo;
        this.ordemRepo = ordemRepo;
        this.vendaRepo = vendaRepo;
        this.usuarioRepo = usuarioRepo;
        this.pagamentoRepo = pagamentoRepo;
        this.empresaRepo = empresaRepo;
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepo.findByUsername(login).orElse(null);
    }

    @GetMapping("/funcionarios")
    public String listarEquipe(Model model) {
        Usuario adminLogado = getUsuarioLogado();
        if (adminLogado == null) return "redirect:/login";

        model.addAttribute("usuario", adminLogado);

        Long empresaId = adminLogado.getEmpresa().getId();

        List<Usuario> usuarios = usuarioRepo.findByEmpresaId(empresaId);
        List<OrdemServico> todasOrdens = ordemRepo.findByEmpresaIdOrderByIdDesc(empresaId);
        List<Venda> todasVendas = vendaRepo.findByEmpresaIdOrderByDataHoraDesc(empresaId);
        List<PagamentoComissao> todosPagamentos = pagamentoRepo.findByEmpresaIdOrderByDataHoraDesc(empresaId);

        for (Usuario u : usuarios) {
            final Long idU = u.getId();
            final String nomeU = (u.getNome() != null) ? u.getNome().trim() : "";

            LocalDateTime corteOs = todosPagamentos.stream()
                    .filter(p -> Objects.equals(p.getFuncionarioId(), idU) && "OS".equals(p.getTipoComissao()))
                    .map(PagamentoComissao::getDataHora)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.of(2000, 1, 1, 0, 0));

            LocalDateTime corteVenda = todosPagamentos.stream()
                    .filter(p -> Objects.equals(p.getFuncionarioId(), idU) && "VENDA".equals(p.getTipoComissao()))
                    .map(PagamentoComissao::getDataHora)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.of(2000, 1, 1, 0, 0));

            List<Venda> vendasPendentes = todasVendas.stream()
                    .filter(v -> v.getVendedor() != null && Objects.equals(v.getVendedor().getId(), idU))
                    .filter(v -> v.getDataHora() != null && v.getDataHora().isAfter(corteVenda))
                    .toList();

            BigDecimal brutoVenda = vendasPendentes.stream()
                    .map(v -> BigDecimal.valueOf(v.getValorTotal() != null ? v.getValorTotal() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal comissaoVenda = vendasPendentes.stream()
                    .map(v -> BigDecimal.valueOf(v.getComissaoVendedorValor() != null ? v.getComissaoVendedorValor() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<OrdemServico> ordensPendentes = todasOrdens.stream()
                    .filter(os -> {
                        String status = os.getStatus() != null ? os.getStatus() : "";
                        boolean concluida = "Entregue".equalsIgnoreCase(status) || "Concluído".equalsIgnoreCase(status);
                        String nomeNaOs = (os.getFuncionarioAndamento() != null) ? os.getFuncionarioAndamento().trim() : "";
                        return concluida && nomeNaOs.equalsIgnoreCase(nomeU);
                    })
                    .filter(os -> os.getDataEntrega() != null && os.getDataEntrega().isAfter(corteOs))
                    .toList();

            BigDecimal brutoOs = ordensPendentes.stream()
                    .map(os -> BigDecimal.valueOf(os.getValorTotal() != null ? os.getValorTotal() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal comissaoOs = ordensPendentes.stream()
                    .map(os -> {
                        if (os.getComissaoTecnicoValor() != null && os.getComissaoTecnicoValor() > 0) {
                            return BigDecimal.valueOf(os.getComissaoTecnicoValor());
                        }
                        BigDecimal liq = BigDecimal.valueOf((os.getValorTotal() != null ? os.getValorTotal() : 0.0) - (os.getCustoPeca() != null ? os.getCustoPeca() : 0.0));
                        BigDecimal taxa = BigDecimal.valueOf(u.getComissaoOs() != null ? u.getComissaoOs() : 0.0);
                        return liq.multiply(taxa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            u.setBrutoOsCalculado(brutoOs.doubleValue());
            u.setBrutoVendaCalculado(brutoVenda.doubleValue());
            u.setTotalComissaoOsAcumulada(comissaoOs.doubleValue());
            u.setSaldoVendaCalculado(comissaoVenda.doubleValue());

            var ultimoPgtoGeral = todosPagamentos.stream()
                    .filter(p -> Objects.equals(p.getFuncionarioId(), idU))
                    .map(PagamentoComissao::getDataHora)
                    .max(LocalDateTime::compareTo);

            if (ultimoPgtoGeral.isPresent()) {
                u.setDataUltimoPagamento(ultimoPgtoGeral.get());
                u.setDiasSemPagamento(ChronoUnit.DAYS.between(ultimoPgtoGeral.get(), LocalDateTime.now()));
            }
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("pagamentos", todosPagamentos);

        return "funcionarios";
    }

    @PostMapping("/empresa/gerar-renovacao")
    @ResponseBody
    public ResponseEntity<?> gerarPixRenovacao(@RequestParam("dias") int dias) {
        try {
            Usuario admin = getUsuarioLogado();
            if (admin == null || admin.getEmpresa() == null) return ResponseEntity.status(403).build();

            MercadoPagoConfig.setAccessToken(mpAccessToken.trim());
            PaymentClient client = new PaymentClient();

            BigDecimal valorTotal = BigDecimal.valueOf(dias).multiply(new BigDecimal("2.00"));

            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorTotal)
                    .description("Renovação Shark: " + dias + " dias - " + admin.getEmpresa().getNome())
                    .paymentMethodId("pix")
                    .externalReference(admin.getEmpresa().getId() + ":" + dias)
                    .payer(PaymentPayerRequest.builder()
                            .email(admin.getEmail())
                            .firstName(admin.getNome())
                            .identification(IdentificationRequest.builder()
                                    .type("CPF")
                                    .number(admin.getCpf().replaceAll("\\D", ""))
                                    .build())
                            .build())
                    .build();

            Payment payment = client.create(paymentRequest);

            return ResponseEntity.ok(Map.of(
                    "qr_code", payment.getPointOfInteraction().getTransactionData().getQrCode(),
                    "qr_code_base64", payment.getPointOfInteraction().getTransactionData().getQrCodeBase64(),
                    "dias_anteriores", admin.getEmpresa().getDiasRestantes()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao gerar Pix");
        }
    }

    @PostMapping("/empresa/atualizar-cnpj")
    public String atualizarCnpj(@RequestParam("cnpj") String cnpj, RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        if (admin != null && admin.getEmpresa() != null) {
            Empresa emp = admin.getEmpresa();
            emp.setCnpj(cnpj.replaceAll("\\D", ""));
            empresaRepo.save(emp);
            ra.addFlashAttribute("mensagem", "CNPJ da empresa atualizado com sucesso!");
        }
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/configurar/{id}")
    public String configurarFuncionario(@PathVariable Long id,
                                        @RequestParam("tipoFuncionario") String tipoFuncionario,
                                        @RequestParam("comissaoOs") Double comissaoOs,
                                        @RequestParam("comissaoVenda") Double comissaoVenda,
                                        RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        usuarioRepo.findById(id).ifPresent(u -> {
            if (u.getEmpresa().getId().equals(admin.getEmpresa().getId())) {
                u.setTipoFuncionario(tipoFuncionario);
                u.setComissaoOs(comissaoOs);
                u.setComissaoVenda(comissaoVenda);
                usuarioRepo.save(u);
                ra.addFlashAttribute("mensagem", "Configurações atualizadas!");
            }
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/aprovar/{id}")
    public String aprovarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        usuarioRepo.findById(id).ifPresent(u -> {
            if (u.getEmpresa().getId().equals(admin.getEmpresa().getId())) {
                u.setAprovado(true);
                usuarioRepo.save(u);
                ra.addFlashAttribute("mensagem", "Funcionário aprovado!");
            }
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/deletar/{id}")
    public String deletarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        usuarioRepo.findById(id).ifPresent(u -> {
            if (u.getEmpresa().getId().equals(admin.getEmpresa().getId())) {
                usuarioRepo.delete(u);
                ra.addFlashAttribute("mensagem", "Funcionário removido.");
            }
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id, RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        clienteRepo.findById(id).ifPresent(c -> {
            if (c.getEmpresa().getId().equals(admin.getEmpresa().getId())) {
                clienteRepo.delete(c);
            }
        });
        return "redirect:/clientes";
    }

    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id, RedirectAttributes ra) {
        Usuario admin = getUsuarioLogado();
        ordemRepo.findById(id).ifPresent(os -> {
            if (os.getEmpresa().getId().equals(admin.getEmpresa().getId())) {
                ordemRepo.delete(os);
            }
        });
        return "redirect:/ordens";
    }
}