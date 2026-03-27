package com.assistencia.controller;

import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // --- REFERÊNCIA DE HOJE ---
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.atTime(LocalTime.MAX);

        // 1. CONTADORES BÁSICOS (HOJE)
        model.addAttribute("clientesHoje", clienteRepository.countByDataCadastroBetween(inicioHoje, fimHoje));
        model.addAttribute("osCriadasHoje", ordemServicoRepository.countByDataBetween(inicioHoje, fimHoje));
        model.addAttribute("vendasHoje", vendaRepository.countByDataHoraBetween(inicioHoje, fimHoje));
        model.addAttribute("osEntreguesHoje", ordemServicoRepository.countByStatusAndDataEntregaBetween("Entregue", inicioHoje, fimHoje));

        // 2. FINANCEIRO: VENDAS (PRODUTOS)
        // Valor total vendido hoje (Bruto)
        Double totalVendasBruto = vendaRepository.somarVendasDoDia(inicioHoje, fimHoje);
        totalVendasBruto = (totalVendasBruto != null) ? totalVendasBruto : 0.0;

        // Custo de aquisição dos produtos vendidos (Estoque)
        Double custoEstoque = vendaRepository.somarCustoEstoqueDasVendasDoDia(inicioHoje, fimHoje);
        custoEstoque = (custoEstoque != null) ? custoEstoque : 0.0;

        Double vendaLiquida = totalVendasBruto - custoEstoque;

        model.addAttribute("totalVendasValorHoje", totalVendasBruto);
        model.addAttribute("custoEstoqueHoje", custoEstoque);
        model.addAttribute("vendaLiquidaHoje", vendaLiquida);

        // 3. FINANCEIRO: SERVIÇOS (ORDENS DE SERVIÇO)
        // Valor total das OS entregues hoje (Bruto)
        Double totalOsBruto = ordemServicoRepository.somarValorBrutoOsEntregues("Entregue", inicioHoje, fimHoje);
        totalOsBruto = (totalOsBruto != null) ? totalOsBruto : 0.0;

        // Total gasto com peças nas OS entregues hoje
        Double totalGastoPecas = ordemServicoRepository.somarCustoPecasOsEntregues("Entregue", inicioHoje, fimHoje);
        totalGastoPecas = (totalGastoPecas != null) ? totalGastoPecas : 0.0;

        Double servicoLiquido = totalOsBruto - totalGastoPecas;

        model.addAttribute("totalServicosHoje", totalOsBruto);
        model.addAttribute("totalGastoPecasHoje", totalGastoPecas);
        model.addAttribute("servicoLiquidoHoje", servicoLiquido);

        // 4. RESULTADO CONSOLIDADO (LUCRO TOTAL)
        model.addAttribute("lucroTotalHoje", vendaLiquida + servicoLiquido);

        return "dashboard";
    }
}