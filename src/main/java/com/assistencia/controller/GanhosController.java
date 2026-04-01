package com.assistencia.controller;

import com.assistencia.model.OrdemServico;
import com.assistencia.model.Usuario;
import com.assistencia.model.Venda;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class GanhosController {

    private final VendaRepository vendaRepo;
    private final OrdemServicoRepository ordemRepo;
    private final UsuarioRepository usuarioRepo;

    public GanhosController(VendaRepository vendaRepo, OrdemServicoRepository ordemRepo, UsuarioRepository usuarioRepo) {
        this.vendaRepo = vendaRepo;
        this.ordemRepo = ordemRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping("/meus-ganhos")
    public String meusGanhos(Model model, Authentication auth) {
        // 1. Identifica o usuário logado
        Usuario usuarioLogado = usuarioRepo.findByUsername(auth.getName()).orElse(null);

        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        Long idU = usuarioLogado.getId();
        String nomeU = (usuarioLogado.getNome() != null) ? usuarioLogado.getNome().trim() : "";

        // 2. Filtra todas as VENDAS realizadas por este usuário
        List<Venda> todasVendas = vendaRepo.findAll();
        List<Venda> minhasVendas = todasVendas.stream()
                .filter(v -> v.getVendedor() != null && Objects.equals(v.getVendedor().getId(), idU))
                .collect(Collectors.toList());

        // --- CORREÇÃO AQUI: Somar o faturamento BRUTO das vendas ---
        double faturamentoBrutoVendas = minhasVendas.stream()
                .mapToDouble(v -> v.getValorTotal() != null ? v.getValorTotal() : 0.0)
                .sum();

        // Soma as comissões de vendas
        double totalComissaoVendas = minhasVendas.stream()
                .mapToDouble(v -> v.getComissaoVendedorValor() != null ? v.getComissaoVendedorValor() : 0.0)
                .sum();

        // 3. Filtra as ORDENS DE SERVIÇO concluídas
        List<OrdemServico> todasOrdens = ordemRepo.findAll();
        List<OrdemServico> meusServicos = todasOrdens.stream()
                .filter(os -> ("Entregue".equalsIgnoreCase(os.getStatus()) || "Concluído".equalsIgnoreCase(os.getStatus())) &&
                        os.getFuncionarioAndamento() != null &&
                        os.getFuncionarioAndamento().trim().equalsIgnoreCase(nomeU))
                .collect(Collectors.toList());

        // Calcula o lucro bruto da O.S. (Valor Total - Custo da Peça)
        double totalBrutoServicos = meusServicos.stream()
                .mapToDouble(os -> (os.getValorTotal() != null ? os.getValorTotal() : 0.0) - (os.getCustoPeca() != null ? os.getCustoPeca() : 0.0))
                .sum();

        // Aplica a porcentagem de comissão do técnico
        double taxaOs = (usuarioLogado.getComissaoOs() != null) ? usuarioLogado.getComissaoOs() : 0.0;
        double totalComissaoOs = (totalBrutoServicos * taxaOs) / 100.0;

        // 4. SETA OS VALORES (O PULO DO GATO PARA SAIR DO ZERO) 🦈
        usuarioLogado.setBrutoVendaCalculado(faturamentoBrutoVendas); // Agora o card de faturamento vai aparecer!
        usuarioLogado.setSaldoVendaCalculado(totalComissaoVendas);
        usuarioLogado.setTotalComissaoOsAcumulada(totalComissaoOs);

        // 5. Envia os dados para a página
        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("vendas", minhasVendas);
        model.addAttribute("servicos", meusServicos);
        model.addAttribute("totalGeral", totalComissaoVendas + totalComissaoOs);

        return "ganhos";
    }
}