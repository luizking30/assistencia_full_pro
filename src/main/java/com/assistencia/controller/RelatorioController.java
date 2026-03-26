package com.assistencia.controller;

import com.assistencia.model.Produto;
import com.assistencia.model.Venda;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.ProdutoRepository;
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

@Controller
public class RelatorioController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private OrdemServicoRepository osRepository;

    @GetMapping("/relatorios")
    public String gerarRelatorioCompleto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {

        List<Produto> produtos = produtoRepository.findAll();
        List<Venda> vendas;
        List<OrdemServico> servicos;

        // 1. Lógica de Filtro por Data com Ordenação Recente
        if (inicio != null && fim != null) {
            LocalDateTime dataInicial = inicio.atStartOfDay();
            LocalDateTime dataFinal = fim.atTime(LocalTime.MAX);

            // Ajuste: Busca vendas no período (Certifique-se que seu VendaRepository tenha esse método ordenado)
            vendas = vendaRepository.findByDataHoraBetween(dataInicial, dataFinal);

            // Ajuste: Busca serviços entregues usando o novo método ordenado do Repository
            servicos = osRepository.findByStatusAndDataEntregaBetweenOrderByIdDesc("Entregue", dataInicial, dataFinal);
        } else {
            // Ajuste: Busca tudo usando a ordenação padrão definida no Repository
            vendas = vendaRepository.findAll();
            servicos = osRepository.findByStatusOrderByIdDesc("Entregue");
        }

        // 2. Cálculo de Estoque (Custo Total parado na loja)
        double totalInvestido = produtos.stream()
                .filter(p -> p.getPrecoCusto() != null && p.getQuantidade() != null)
                .mapToDouble(p -> p.getPrecoCusto() * p.getQuantidade())
                .sum();

        // 3. Faturamento de Vendas (Produtos)
        double totalVendasValor = vendas.stream()
                .mapToDouble(v -> v.getValorTotal() != null ? v.getValorTotal() : 0.0)
                .sum();

        // 4. Faturamento de Serviços (Mão de obra)
        double totalServicosValor = servicos.stream()
                .mapToDouble(s -> s.getValorTotal() != null ? s.getValorTotal() : 0.0)
                .sum();

        // Faturamento Bruto (Soma de tudo que entrou)
        double faturamentoBruto = totalVendasValor + totalServicosValor;

        // Balanço Líquido (Faturamento - Custo de Aquisição do Estoque)
        double lucroLiquido = faturamentoBruto - totalInvestido;

        // Enviando dados para o Thymeleaf
        model.addAttribute("vendas", vendas);
        model.addAttribute("servicos", servicos);
        model.addAttribute("totalInvestido", totalInvestido);
        model.addAttribute("totalVendido", faturamentoBruto);
        model.addAttribute("lucroLiquido", lucroLiquido);

        // Mantém as datas nos campos de filtro da página
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);

        return "relatorios";
    }
}