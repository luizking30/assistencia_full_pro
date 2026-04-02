package com.assistencia.controller;

import com.assistencia.model.OrdemServico;
import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.model.Venda;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    public AdminController(ClienteRepository clienteRepo, OrdemServicoRepository ordemRepo,
                           VendaRepository vendaRepo, UsuarioRepository usuarioRepo,
                           PagamentoComissaoRepository pagamentoRepo) {
        this.clienteRepo = clienteRepo;
        this.ordemRepo = ordemRepo;
        this.vendaRepo = vendaRepo;
        this.usuarioRepo = usuarioRepo;
        this.pagamentoRepo = pagamentoRepo;
    }

    @GetMapping("/funcionarios")
    public String listarEquipe(Model model) {
        List<Usuario> usuarios = usuarioRepo.findAll();
        List<OrdemServico> todasOrdens = ordemRepo.findAll();
        List<Venda> todasVendas = vendaRepo.findAll();
        List<PagamentoComissao> todosPagamentos = pagamentoRepo.findAll();

        for (Usuario u : usuarios) {
            final Long idU = u.getId();
            final String nomeU = (u.getNome() != null) ? u.getNome().trim() : "";

            // 1. DETERMINAR DATAS DE CORTE (ÚLTIMOS PAGAMENTOS POR TIPO)
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

            // 2. CÁLCULO VENDAS (SÓ O QUE FOI FEITO APÓS O CORTE)
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

            // 3. CÁLCULO O.S. (SÓ O QUE FOI FEITO APÓS O CORTE)
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
                        BigDecimal liq = BigDecimal.valueOf(os.getValorTotal() - (os.getCustoPeca() != null ? os.getCustoPeca() : 0.0));
                        BigDecimal taxa = BigDecimal.valueOf(u.getComissaoOs() != null ? u.getComissaoOs() : 0.0);
                        return liq.multiply(taxa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. SETANDO VALORES PARA O HTML
            u.setBrutoOsCalculado(brutoOs.doubleValue());
            u.setBrutoVendaCalculado(brutoVenda.doubleValue());
            u.setTotalComissaoOsAcumulada(comissaoOs.doubleValue());
            u.setSaldoVendaCalculado(comissaoVenda.doubleValue());

            // 5. ÚLTIMO PAGAMENTO GERAL (AUDITORIA)
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
        // Envia os pagamentos para o HTML poder "pescar" as datas individuais
        model.addAttribute("pagamentos", pagamentoRepo.findTop50ByOrderByDataHoraDesc());

        return "funcionarios";
    }

    @PostMapping("/funcionarios/configurar/{id}")
    public String configurarFuncionario(@PathVariable Long id,
                                        @RequestParam("tipoFuncionario") String tipoFuncionario,
                                        @RequestParam("comissaoOs") Double comissaoOs,
                                        @RequestParam("comissaoVenda") Double comissaoVenda,
                                        RedirectAttributes ra) {
        usuarioRepo.findById(id).ifPresent(u -> {
            u.setTipoFuncionario(tipoFuncionario);
            u.setComissaoOs(comissaoOs);
            u.setComissaoVenda(comissaoVenda);
            usuarioRepo.save(u);
            ra.addFlashAttribute("mensagem", "Configurações atualizadas!");
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/aprovar/{id}")
    public String aprovarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.findById(id).ifPresent(u -> {
            u.setAprovado(true);
            usuarioRepo.save(u);
            ra.addFlashAttribute("mensagem", "Funcionário aprovado!");
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/deletar/{id}")
    public String deletarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        if (usuarioRepo.existsById(id)) {
            usuarioRepo.deleteById(id);
            ra.addFlashAttribute("mensagem", "Funcionário removido.");
        }
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id, RedirectAttributes ra) {
        clienteRepo.deleteById(id);
        return "redirect:/clientes";
    }

    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id, RedirectAttributes ra) {
        ordemRepo.deleteById(id);
        return "redirect:/ordens";
    }
}