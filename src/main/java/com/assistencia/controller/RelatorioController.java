package com.assistencia.controller;

import com.assistencia.model.Venda;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

@Controller
public class RelatorioController {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private OrdemServicoRepository osRepository;

    @GetMapping("/relatorios")
    public String gerarRelatorioCompleto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {

        List<Venda> vendas = new ArrayList<>();
        List<OrdemServico> servicos = new ArrayList<>();

        // Inicializamos os totais com 0.0 para evitar erros de exibição no HTML
        double totalVendasBruto = 0.0;
        double custoEstoqueVendido = 0.0;
        double totalServicosBruto = 0.0;
        double custoPecasOS = 0.0;

        if (inicio != null && fim != null) {
            LocalDateTime dataInicial = inicio.atStartOfDay();
            LocalDateTime dataFinal = fim.atTime(LocalTime.MAX);

            // 1. Busca no Banco
            vendas = vendaRepository.findByDataHoraBetween(dataInicial, dataFinal);
            servicos = osRepository.findByStatusAndDataEntregaBetween("Entregue", dataInicial, dataFinal);

            // 2. Cálculos de Vendas (Produtos)
            totalVendasBruto = vendas.stream()
                    .mapToDouble(v -> v.getValorTotal() != null ? v.getValorTotal() : 0.0).sum();

            custoEstoqueVendido = vendas.stream()
                    .mapToDouble(v -> v.getCustoTotalEstoque() != null ? v.getCustoTotalEstoque() : 0.0).sum();

            // 3. Cálculos de Serviços (OS)
            totalServicosBruto = servicos.stream()
                    .mapToDouble(s -> s.getValorTotal() != null ? s.getValorTotal() : 0.0).sum();

            custoPecasOS = servicos.stream()
                    .mapToDouble(s -> s.getCustoPeca() != null ? s.getCustoPeca() : 0.0).sum();
        }

        // 4. Totais Gerais e Lucros (Calculados fora do IF para evitar erros no Model)
        double lucroVendas = totalVendasBruto - custoEstoqueVendido;
        double lucroServicos = totalServicosBruto - custoPecasOS;
        double lucroTotalPeriodo = lucroVendas + lucroServicos;

        // Atributos para os Cards do Thymeleaf
        model.addAttribute("totalVendasBruto", totalVendasBruto);
        model.addAttribute("custoEstoqueVendido", custoEstoqueVendido);
        model.addAttribute("lucroVendas", lucroVendas);

        model.addAttribute("totalServicosBruto", totalServicosBruto);
        model.addAttribute("custoPecasOS", custoPecasOS);
        model.addAttribute("lucroServicos", lucroServicos);

        // Atributo principal do Resultado Total (Linha 163 do seu HTML)
        model.addAttribute("lucroTotalPeriodo", lucroTotalPeriodo);

        // Listas e Datas para a tabela e o formulário
        model.addAttribute("vendas", vendas);
        model.addAttribute("servicos", servicos);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);

        return "relatorios";
    }
}