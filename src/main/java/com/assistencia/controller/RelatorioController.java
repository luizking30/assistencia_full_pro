package com.assistencia.controller;

import com.assistencia.model.Venda;
import com.assistencia.model.OrdemServico;
import com.assistencia.model.Conta;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.ContaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
public class RelatorioController {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private OrdemServicoRepository osRepository;

    @Autowired
    private ContaRepository contaRepository;

    @GetMapping("/relatorios")
    public String gerarRelatorioCompleto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String mesFiltro,
            Model model) {

        List<Venda> vendas = new ArrayList<>();
        List<OrdemServico> servicos = new ArrayList<>();
        List<Conta> contasPagas = new ArrayList<>();

        double totalVendasBruto = 0.0;
        double custoEstoqueVendido = 0.0;
        double totalServicosBruto = 0.0;
        double custoPecasOS = 0.0;
        double totalDespesas = 0.0;

        // 🚀 LÓGICA DE MÊS: Se o usuário filtrou por mês, convertemos para início e fim do mês
        if (mesFiltro != null && !mesFiltro.isEmpty()) {
            YearMonth ym = YearMonth.parse(mesFiltro);
            inicio = ym.atDay(1);
            fim = ym.atEndOfMonth();
            model.addAttribute("mesFiltro", mesFiltro);
        }

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

            // 4. Busca e Cálculo de Despesas (Contas Pagas no período)
            LocalDate dataIniContas = inicio;
            LocalDate dataFimContas = fim;
            contasPagas = contaRepository.findAll().stream()
                    .filter(c -> c.isPaga() && c.getDataPagamento() != null &&
                            !c.getDataPagamento().toLocalDate().isBefore(dataIniContas) &&
                            !c.getDataPagamento().toLocalDate().isAfter(dataFimContas))
                    .collect(Collectors.toList());

            totalDespesas = contasPagas.stream().mapToDouble(Conta::getValor).sum();
        }

        // 5. Totais Gerais e Lucros
        double lucroVendas = totalVendasBruto - custoEstoqueVendido;
        double lucroServicos = totalServicosBruto - custoPecasOS;
        double lucroTotalPeriodo = lucroVendas + lucroServicos;
        double lucroTotalFinal = lucroTotalPeriodo - totalDespesas; // Lucro Real (Vendas + OS - Contas)

        // Atributos para o Thymeleaf
        model.addAttribute("totalVendasBruto", totalVendasBruto);
        model.addAttribute("custoEstoqueVendido", custoEstoqueVendido);
        model.addAttribute("lucroVendas", lucroVendas);

        model.addAttribute("totalServicosBruto", totalServicosBruto);
        model.addAttribute("custoPecasOS", custoPecasOS);
        model.addAttribute("lucroServicos", lucroServicos);

        model.addAttribute("totalDespesas", totalDespesas);
        model.addAttribute("lucroTotalPeriodo", lucroTotalPeriodo); // Vendas + Serviços
        model.addAttribute("lucroTotalFinal", lucroTotalFinal);     // Vendas + Serviços - Contas

        model.addAttribute("vendas", vendas);
        model.addAttribute("servicos", servicos);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);

        return "relatorios";
    }
}