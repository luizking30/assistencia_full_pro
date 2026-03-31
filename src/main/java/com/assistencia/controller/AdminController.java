package com.assistencia.controller;

import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.model.Venda;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

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
        List<PagamentoComissao> pagamentos = pagamentoRepo.findAll();

        // Busca todas as Ordens e Vendas para processar em memória
        List<OrdemServico> todasOrdens = ordemRepo.findAll();
        List<Venda> todasVendas = vendaRepo.findAll();

        Map<Long, Double> totalServicos = new HashMap<>();
        Map<Long, Double> totalVendas = new HashMap<>();

        for (Usuario u : usuarios) {
            final String nomeUsuarioDb = (u.getNome() != null) ? u.getNome().trim() : "";

            // 1. CÁLCULO COMISSÃO O.S. (TÉCNICO)
            double comissaoTecnico = todasOrdens.stream()
                    .filter(os -> "Entregue".equalsIgnoreCase(os.getStatus())) // Verifique se no banco está "Entregue"
                    .filter(os -> {
                        String funcOS = os.getFuncionarioAndamento();
                        boolean bate = funcOS != null && funcOS.trim().equalsIgnoreCase(nomeUsuarioDb);
                        if (bate) {
                            System.out.println("DEBUG: OS #" + os.getId() + " vinculada ao técnico " + nomeUsuarioDb);
                        }
                        return bate;
                    })
                    .mapToDouble(os -> {
                        double valorTotal = os.getValorTotal() != null ? os.getValorTotal() : 0.0;
                        double custoPeca = os.getCustoPeca() != null ? os.getCustoPeca() : 0.0;
                        double percentualOS = u.getComissaoOs() != null ? u.getComissaoOs() : 0.0;

                        // Cálculo sobre o lucro bruto (Serviço)
                        return (valorTotal - custoPeca) * (percentualOS / 100.0);
                    })
                    .sum();

            // 2. CÁLCULO COMISSÃO VENDAS (VENDEDOR)
            double comissaoVendaBalcao = todasVendas.stream()
                    .filter(v -> v.getVendedor() != null && v.getVendedor().getId().equals(u.getId()))
                    .mapToDouble(v -> {
                        double valorVenda = v.getValorTotal() != null ? v.getValorTotal() : 0.0;
                        double percentualVen = u.getComissaoVenda() != null ? u.getComissaoVenda() : 0.0;
                        return valorVenda * (percentualVen / 100.0);
                    })
                    .sum();

            totalServicos.put(u.getId(), comissaoTecnico);
            totalVendas.put(u.getId(), comissaoVendaBalcao);
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("pagamentos", pagamentos);
        model.addAttribute("totalServicos", totalServicos);
        model.addAttribute("totalVendas", totalVendas);

        return "funcionarios";
    }

    @PostMapping("/funcionarios/configurar/{id}")
    public String configurarFuncionario(@PathVariable Long id,
                                        @RequestParam("tipoFuncionario") String tipo,
                                        @RequestParam("comissaoOs") Double comissaoOs,
                                        @RequestParam("comissaoVenda") Double comissaoVenda,
                                        RedirectAttributes ra) {

        usuarioRepo.findById(id).ifPresent(usuario -> {
            usuario.setTipoFuncionario(tipo);
            usuario.setComissaoOs(comissaoOs);
            usuario.setComissaoVenda(comissaoVenda);
            usuarioRepo.save(usuario);
            ra.addFlashAttribute("mensagem", "Configurações de " + usuario.getNome() + " atualizadas!");
        });
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/funcionarios/aprovar/{id}")
    public String aprovarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.findById(id).ifPresent(usuario -> {
            usuario.setAprovado(true);
            usuarioRepo.save(usuario);
            ra.addFlashAttribute("mensagem", "Funcionário " + usuario.getNome() + " aprovado!");
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

    // Métodos para deleção de clientes e ordens (ajustados para as rotas corretas)
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