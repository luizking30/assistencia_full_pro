package com.assistencia.controller;

import com.assistencia.model.OrdemServico;
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

        for (Usuario u : usuarios) {
            final String nomeUsuarioDb = (u.getNome() != null) ? u.getNome().trim() : "";
            final Long idUsuario = u.getId();

            // 1. CÁLCULO VENDAS COM BIGDECIMAL 📈
            List<Venda> vendasDesteU = todasVendas.stream()
                    .filter(v -> v.getVendedor() != null && Objects.equals(v.getVendedor().getId(), idUsuario))
                    .toList();

            BigDecimal faturamentoBruto = vendasDesteU.stream()
                    .map(v -> BigDecimal.valueOf(v.getValorTotal() != null ? v.getValorTotal() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal taxaVenda = BigDecimal.valueOf(u.getComissaoVenda() != null ? u.getComissaoVenda() : 0.0);

            // Conta: (Faturamento * Taxa) / 100
            BigDecimal comissaoGeradaVenda = faturamentoBruto.multiply(taxaVenda)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // 2. CÁLCULO O.S. COM BIGDECIMAL 🛠️
            BigDecimal comissaoGeradaOs = todasOrdens.stream()
                    .filter(os -> {
                        String status = os.getStatus();
                        boolean concluida = "Entregue".equalsIgnoreCase(status) || "Concluído".equalsIgnoreCase(status);
                        boolean mesmoFuncionario = os.getFuncionarioAndamento() != null &&
                                os.getFuncionarioAndamento().trim().equalsIgnoreCase(nomeUsuarioDb);
                        return concluida && mesmoFuncionario;
                    })
                    .map(os -> {
                        BigDecimal valorOs = BigDecimal.valueOf(os.getValorTotal() != null ? os.getValorTotal() : 0.0);
                        BigDecimal custoPeca = BigDecimal.valueOf(os.getCustoPeca() != null ? os.getCustoPeca() : 0.0);
                        BigDecimal taxaOs = BigDecimal.valueOf(u.getComissaoOs() != null ? u.getComissaoOs() : 0.0);
                        // (Total - Peça) * Taxa / 100
                        return valorOs.subtract(custoPeca)
                                .multiply(taxaOs)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. ABATIMENTO DE PAGAMENTOS
            BigDecimal valorPagoOS = BigDecimal.valueOf(Objects.requireNonNullElse(pagamentoRepo.somarPorFuncionarioETipo(idUsuario, "OS"), 0.0));
            BigDecimal valorPagoVenda = BigDecimal.valueOf(Objects.requireNonNullElse(pagamentoRepo.somarPorFuncionarioETipo(idUsuario, "VENDA"), 0.0));

            // Saldo Final (Garante que não seja negativo)
            BigDecimal saldoFinalVenda = comissaoGeradaVenda.subtract(valorPagoVenda).max(BigDecimal.ZERO);
            BigDecimal saldoFinalOS = comissaoGeradaOs.subtract(valorPagoOS).max(BigDecimal.ZERO);

            // 🚀 SETANDO NOS CAMPOS TRANSIENTES (Convertendo de volta para double para o HTML)
            u.setBrutoVendaCalculado(faturamentoBruto.doubleValue());
            u.setSaldoVendaCalculado(saldoFinalVenda.doubleValue());
            u.setTotalComissaoOsAcumulada(saldoFinalOS.doubleValue());

            // LOG DE PRECISÃO NO CONSOLE
            System.out.println(">>> [SHARK PRECISION] Funcionário: " + nomeUsuarioDb);
            System.out.println("    Faturamento: " + faturamentoBruto + " | Comissão: " + saldoFinalVenda);
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("pagamentos", pagamentoRepo.findTop10ByOrderByDataHoraDesc());

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
            ra.addFlashAttribute("mensagem", "Configurações de " + u.getNome() + " atualizadas!");
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/aprovar/{id}")
    public String aprovarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.findById(id).ifPresent(u -> {
            u.setAprovado(true);
            usuarioRepo.save(u);
            ra.addFlashAttribute("mensagem", "Funcionário " + u.getNome() + " aprovado!");
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/deletar/{id}")
    public String deletarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        if (usuarioRepo.existsById(id)) {
            usuarioRepo.deleteById(id);
            ra.addFlashAttribute("mensagem", "Funcionário removido com sucesso.");
        }
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id, RedirectAttributes ra) {
        clienteRepo.deleteById(id);
        ra.addFlashAttribute("mensagem", "Cliente removido.");
        return "redirect:/clientes";
    }

    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id, RedirectAttributes ra) {
        ordemRepo.deleteById(id);
        ra.addFlashAttribute("mensagem", "Ordem de Serviço excluída.");
        return "redirect:/ordens";
    }
}