package com.assistencia.controller;

import com.assistencia.model.Produto;
import com.assistencia.repository.ProdutoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoRepository repo;

    public ProdutoController(ProdutoRepository repo) {
        this.repo = repo;
    }

    // =========================
    // Listar produtos - página /produtos
    // =========================
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", repo.findAll());
        return "produtos"; // produtos.html
    }

    // =========================
    // Salvar produto via formulário tradicional
    // =========================
    @PostMapping
    public String salvar(@RequestParam String nome,
                         @RequestParam Double preco,
                         @RequestParam Integer quantidade) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setPreco(preco);
        p.setQuantidade(quantidade);
        repo.save(p);
        return "redirect:/produtos";
    }

    // =========================
    // Salvar produto via modal no dashboard (AJAX)
    // =========================
    @PostMapping("/dashboard")
    @ResponseBody
    public String salvarDashboard(@RequestParam String nome,
                                  @RequestParam Double preco,
                                  @RequestParam Integer quantidade) {
        try {
            Produto p = new Produto();
            p.setNome(nome);
            p.setPreco(preco);
            p.setQuantidade(quantidade);
            repo.save(p);
            return "ok";
        } catch (Exception e) {
            e.printStackTrace(); // exibe no console em caso de erro
            return "erro";
        }
    }

}