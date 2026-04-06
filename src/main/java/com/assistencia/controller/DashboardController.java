package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
public class DashboardController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // 1. RECUPERAR EMPRESA DO USUÁRIO LOGADO
        Usuario logado = getUsuarioLogado();
        if (logado == null || logado.getEmpresa() == null) {
            return "redirect:/login";
        }
        Long empresaId = logado.getEmpresa().getId();

        // --- REFERÊNCIA DE HOJE ---
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.atTime(LocalTime.MAX);

        // 2. CONTADORES BÁSICOS (FILTRADOS POR EMPRESA)
        // Usando os novos nomes de métodos que definimos no Repository
        model.addAttribute("clientesHoje",
                clienteRepository.countByEmpresaIdAndDataCadastroBetween(empresaId, inicioHoje, fimHoje));

        model.addAttribute("osCriadasHoje",
                ordemServicoRepository.countByEmpresaIdAndDataBetween(empresaId, inicioHoje, fimHoje));

        model.addAttribute("vendasHoje",
                vendaRepository.countByEmpresaIdAndDataHoraBetween(empresaId, inicioHoje, fimHoje));

        model.addAttribute("osEntreguesHoje",
                ordemServicoRepository.countByEmpresaIdAndStatusAndDataEntregaBetween(empresaId, "Entregue", inicioHoje, fimHoje));

        // 3. FINANCEIRO: VENDAS (PRODUTOS)
        Double totalVendasBruto = vendaRepository.somarVendasDoDia(empresaId, inicioHoje, fimHoje);
        totalVendasBruto = (totalVendasBruto != null) ? totalVendasBruto : 0.0;

        Double custoEstoque = vendaRepository.somarCustoEstoqueDasVendasDoDia(empresaId, inicioHoje, fimHoje);
        custoEstoque = (custoEstoque != null) ? custoEstoque : 0.0;

        Double vendaLiquida = totalVendasBruto - custoEstoque;

        model.addAttribute("totalVendasValorHoje", totalVendasBruto);
        model.addAttribute("custoEstoqueHoje", custoEstoque);
        model.addAttribute("vendaLiquidaHoje", vendaLiquida);

        // 4. FINANCEIRO: SERVIÇOS (ORDENS DE SERVIÇO)
        Double totalOsBruto = ordemServicoRepository.somarValorBrutoOsEntregues(empresaId, "Entregue", inicioHoje, fimHoje);
        totalOsBruto = (totalOsBruto != null) ? totalOsBruto : 0.0;

        Double totalGastoPecas = ordemServicoRepository.somarCustoPecasOsEntregues(empresaId, "Entregue", inicioHoje, fimHoje);
        totalGastoPecas = (totalGastoPecas != null) ? totalGastoPecas : 0.0;

        Double servicoLiquido = totalOsBruto - totalGastoPecas;

        model.addAttribute("totalServicosHoje", totalOsBruto);
        model.addAttribute("totalGastoPecasHoje", totalGastoPecas);
        model.addAttribute("servicoLiquidoHoje", servicoLiquido);

        // 5. RESULTADO CONSOLIDADO (LUCRO TOTAL)
        model.addAttribute("lucroTotalHoje", vendaLiquida + servicoLiquido);

        return "dashboard";
    }

    // Método auxiliar para pegar o usuário logado
    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepository.findByUsername(login).orElse(null);
    }
}