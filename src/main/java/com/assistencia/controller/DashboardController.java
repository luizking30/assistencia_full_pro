package com.assistencia.controller;

import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository; // <<< IMPORTAÇÃO OBRIGATÓRIA ADICIONADA
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

        // --- DATAS DE REFERÊNCIA ---
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate trintaDiasAtras = hoje.minusDays(30);

        // 1. PROCESSAMENTO: HOJE
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.atTime(LocalTime.MAX);
        processarDados(model, "Hoje", inicioHoje, fimHoje);

        // 2. PROCESSAMENTO: ONTEM
        LocalDateTime inicioOntem = ontem.atStartOfDay();
        LocalDateTime fimOntem = ontem.atTime(LocalTime.MAX);
        processarDados(model, "Ontem", inicioOntem, fimOntem);

        // 3. PROCESSAMENTO: 30 DIAS
        LocalDateTime inicio30 = trintaDiasAtras.atStartOfDay();
        LocalDateTime fim30 = hoje.atTime(LocalTime.MAX);
        processarDados(model, "30Dias", inicio30, fim30);

        return "dashboard";
    }

    /**
     * Método auxiliar para evitar repetição de código e popular o model dinamicamente
     */
    private void processarDados(Model model, String sufixo, LocalDateTime inicio, LocalDateTime fim) {

        // Contadores gerais
        model.addAttribute("clientes" + sufixo, clienteRepository.count());

        // OS Criadas (Usa a data de abertura)
        model.addAttribute("osCriadas" + sufixo, ordemServicoRepository.countByDataBetween(inicio, fim));

        // Vendas de Produtos
        model.addAttribute("vendas" + sufixo, vendaRepository.countByDataHoraBetween(inicio, fim));

        // OS Entregues (Usa a data de entrega conforme definimos no seu Repository)
        long entregues = ordemServicoRepository.countByStatusAndDataBetween("ENTREGUE", inicio, fim);
        model.addAttribute("osEntregues" + sufixo, entregues);

        // Financeiro de Serviços (Mão de obra técnica)
        Double totalServicos = ordemServicoRepository.somarValorServicosDoDia("ENTREGUE", inicio, fim);
        totalServicos = (totalServicos != null) ? totalServicos : 0.0;
        model.addAttribute("totalServicos" + sufixo, totalServicos);

        // Financeiro de Vendas (Produtos da loja)
        // Certifique-se que somarVendasDoDia existe no VendaRepository
        Double totalVendasValor = vendaRepository.somarVendasDoDia(inicio, fim);
        totalVendasValor = (totalVendasValor != null) ? totalVendasValor : 0.0;
        model.addAttribute("totalVendasValor" + sufixo, totalVendasValor);

        // Total Faturado Bruto da Shark (Produtos + Serviços)
        model.addAttribute("totalFaturado" + sufixo, totalServicos + totalVendasValor);
    }
}