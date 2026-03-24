package com.assistencia.controller;

import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.ProdutoRepository;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RelatorioController {

    private final VendaRepository vendaRepo;
    private final ProdutoRepository produtoRepo;
    private final ClienteRepository clienteRepo;
    private final OrdemServicoRepository ordemRepo;

    public RelatorioController(VendaRepository vendaRepo, ProdutoRepository produtoRepo,
                               ClienteRepository clienteRepo, OrdemServicoRepository ordemRepo) {
        this.vendaRepo = vendaRepo;
        this.produtoRepo = produtoRepo;
        this.clienteRepo = clienteRepo;
        this.ordemRepo = ordemRepo;
    }

    @GetMapping("/relatorio")
    public String relatorio(Model model) {

        // Total faturamento (double primitivo não precisa checar null)
        double faturamento = vendaRepo.findAll()
                .stream()
                .mapToDouble(v -> v.getValorVenda()) // valorVenda é double primitivo
                .sum();

        // Total custo (preco * quantidade), double/int primitivo
        double custo = produtoRepo.findAll()
                .stream()
                .mapToDouble(p -> p.getPreco() * p.getQuantidade()) // preco e quantidade são primitivos
                .sum();

        double lucro = faturamento - custo;

        // Totais para dashboard
        long totalClientes = clienteRepo.count();
        long totalOrdens = ordemRepo.count();
        long totalVendas = vendaRepo.count();

        // Adiciona atributos para o Thymeleaf
        model.addAttribute("faturamento", faturamento);
        model.addAttribute("custo", custo);
        model.addAttribute("lucro", lucro);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalOrdens", totalOrdens);
        model.addAttribute("totalVendas", totalVendas);

        return "relatorio"; // arquivo relatorio.html deve existir em templates
    }
}