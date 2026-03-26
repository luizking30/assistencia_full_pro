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

        // Calcula o "Feito do Dia" para a Shark Eletrônicos
        Double totalVendasHoje = vendasHoje.stream()
                .filter(v -> v.getValorTotal() != null)
                .mapToDouble(Venda::getValorTotal)
                .sum();

        model.addAttribute("vendas", vendasHoje);
        model.addAttribute("totalVendasHoje", totalVendasHoje);

        if (!model.containsAttribute("venda")) {
            model.addAttribute("venda", new Venda());
        }
        return "vendas";
    }

    // 🔵 SALVAR NOVA VENDA COM REGISTRO DE VENDEDOR DINÂMICO
    @Transactional
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Venda venda,
                         Authentication auth, // Captura quem está logado (admin, funcionario, etc.)
                         RedirectAttributes redirectAttributes) {
        try {
            List<ItemVenda> itens = venda.getItens();
            if (itens == null || itens.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Adicione pelo menos um produto!");
                return "redirect:/vendas";
            }

            // 1. REGISTRAR QUEM ESTÁ REALIZANDO A VENDA
            if (auth != null && auth.isAuthenticated()) {
                venda.setVendedor(auth.getName()); // Pega o login atual: "admin" ou "funcionario"
            } else {
                venda.setVendedor("Sistema Shark"); // Fallback de segurança
            }

            double valorTotalCalculado = 0.0;

            for (ItemVenda item : itens) {
                Produto produtoOriginal = produtoRepo.findById(item.getProduto().getId())
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado."));

                // Validação de Estoque
                if (produtoOriginal.getQuantidade() < item.getQuantidade()) {
                    redirectAttributes.addFlashAttribute("erro", "Estoque insuficiente: " + produtoOriginal.getNome());
                    return "redirect:/vendas";
                }

                // Garante o preço unitário (usa o do cadastro se não vier da tela)
                if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                    item.setPrecoUnitario(produtoOriginal.getPrecoVenda());
                }

                item.setVenda(venda);
                valorTotalCalculado += item.getPrecoUnitario() * item.getQuantidade();

                // Baixa automática no estoque
                produtoOriginal.setQuantidade(produtoOriginal.getQuantidade() - item.getQuantidade());
                produtoRepo.save(produtoOriginal);
            }

            venda.setValorTotal(valorTotalCalculado);
            venda.setDataHora(LocalDateTime.now());

            vendaRepo.save(venda);
            redirectAttributes.addFlashAttribute("sucesso", "Venda finalizada por " + venda.getVendedor() + "!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("erro", "Erro ao processar venda: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    // 🔴 ESTORNAR VENDA (Devolve os itens para o estoque)
    @Transactional
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Venda venda = vendaRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Venda não encontrada!"));

            // Loop para devolver as quantidades ao estoque da Shark
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
            redirectAttributes.addFlashAttribute("sucesso", "Venda estornada com sucesso!");

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