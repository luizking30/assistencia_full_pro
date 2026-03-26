package com.assistencia.controller;

import com.assistencia.model.ItemVenda;
import com.assistencia.model.Produto;
import com.assistencia.model.Venda;
import com.assistencia.repository.ProdutoRepository;
import com.assistencia.repository.VendaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList; // Importado
import java.util.List;

@Controller
@RequestMapping("/vendas")
public class VendasController {

    private final VendaRepository vendaRepo;
    private final ProdutoRepository produtoRepo;

    public VendasController(VendaRepository vendaRepo, ProdutoRepository produtoRepo) {
        this.vendaRepo = vendaRepo;
        this.produtoRepo = produtoRepo;
    }

    // 🟢 CARREGAR TELA DE VENDAS
    @GetMapping
    public String novaVenda(Model model) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDia = LocalDate.now().atTime(LocalTime.MAX);

        List<Venda> vendasHoje = vendaRepo.findByDataHoraBetween(inicioDia, fimDia);

        Double totalVendasHoje = vendasHoje.stream()
                .filter(v -> v.getValorTotal() != null)
                .mapToDouble(Venda::getValorTotal)
                .sum();

        model.addAttribute("vendas", vendasHoje);
        model.addAttribute("totalVendasHoje", totalVendasHoje);

        // CORREÇÃO 1: Inicializar a lista de itens para o formulário não dar Erro 500
        if (!model.containsAttribute("venda")) {
            Venda novaVenda = new Venda();
            novaVenda.setItens(new ArrayList<>());
            model.addAttribute("venda", novaVenda);
        }
        return "vendas";
    }

    // 🔵 SALVAR NOVA VENDA
    @Transactional
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("venda") Venda venda,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        try {
            // CORREÇÃO 2: Verificar se a lista foi preenchida pelo Thymeleaf
            if (venda.getItens() == null || venda.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "O carrinho está vazio!");
                return "redirect:/vendas";
            }

            // Registrar vendedor
            if (auth != null && auth.isAuthenticated()) {
                venda.setVendedor(auth.getName());
            } else {
                venda.setVendedor("Sistema Shark");
            }

            double valorTotalCalculado = 0.0;

            // Criamos uma lista temporária para evitar erros de concorrência durante o loop
            List<ItemVenda> itensValidados = new ArrayList<>(venda.getItens());

            for (ItemVenda item : itensValidados) {
                if (item.getProduto() == null || item.getProduto().getId() == null) continue;

                Produto produtoOriginal = produtoRepo.findById(item.getProduto().getId())
                        .orElseThrow(() -> new RuntimeException("Produto ID " + item.getProduto().getId() + " não encontrado."));

                // Validação de Estoque
                if (produtoOriginal.getQuantidade() < item.getQuantidade()) {
                    throw new RuntimeException("Estoque insuficiente para: " + produtoOriginal.getNome());
                }

                // Garante o preço
                if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                    item.setPrecoUnitario(produtoOriginal.getPrecoVenda());
                }

                // CORREÇÃO 3: Vínculo bidirecional obrigatório para o JPA
                item.setVenda(venda);

                valorTotalCalculado += item.getPrecoUnitario() * item.getQuantidade();

                // Baixa no estoque
                produtoOriginal.setQuantidade(produtoOriginal.getQuantidade() - item.getQuantidade());
                produtoRepo.save(produtoOriginal);
            }

            venda.setValorTotal(valorTotalCalculado);
            venda.setDataHora(LocalDateTime.now());

            vendaRepo.save(venda);
            redirectAttributes.addFlashAttribute("sucesso", "Venda de R$ " + String.format("%.2f", valorTotalCalculado) + " realizada!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("erro", "Falha na venda: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    // 🔴 ESTORNAR VENDA
    @Transactional
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Venda venda = vendaRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Venda não encontrada!"));

            if (venda.getItens() != null) {
                for (ItemVenda item : venda.getItens()) {
                    Produto produto = item.getProduto();
                    if (produto != null) {
                        produto.setQuantidade(produto.getQuantidade() + item.getQuantidade());
                        produtoRepo.save(produto);
                    }
                }
            }

            vendaRepo.delete(venda);
            redirectAttributes.addFlashAttribute("sucesso", "Venda estornada e estoque devolvido!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao estornar: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    @GetMapping("/historico")
    public String historico(Model model) {
        model.addAttribute("vendas", vendaRepo.findAll());
        return "historico_vendas";
    }
}