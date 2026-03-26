package com.assistencia.controller;

import com.assistencia.model.Produto;
import com.assistencia.repository.ProdutoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final ProdutoRepository repo;

    public EstoqueController(ProdutoRepository repo) {
        this.repo = repo;
    }

    // 1. Listar (Ordenado por ID decrescente para facilitar a visualização na Shark)
    @GetMapping
    public String listar(Model model) {
        List<Produto> produtos = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("produtos", produtos);

        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        return "estoque";
    }

    // 2. Buscar por ID (Usado pelo JavaScript para preencher o formulário de edição)
    @GetMapping("/buscar/{id}")
    @ResponseBody
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔥 NOVO: Sugestões para o Autocomplete da Venda (Estilo Google)
    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Produto> buscarSugestoes(@RequestParam String termo) {
        // Busca produtos que contenham o nome digitado (ignorando maiúsculas/minúsculas)
        // Dica: Limite a 10 resultados para manter a performance da Shark lá no alto
        return repo.findTop10ByNomeContainingIgnoreCase(termo);
    }

    // 3. Salvar ou Atualizar
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Produto produto, RedirectAttributes redirectAttributes) {
        try {
            if (produto.getNome() == null || produto.getNome().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "O nome do produto é obrigatório!");
                return "redirect:/estoque";
            }

            boolean isEdicao = (produto.getId() != null);
            repo.save(produto);

            String mensagem = isEdicao ? "Produto atualizado com sucesso!" : "Produto cadastrado com sucesso!";
            redirectAttributes.addFlashAttribute("sucesso", mensagem);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("erro", "Erro técnico ao salvar: " + e.getMessage());
        }
        return "redirect:/estoque";
    }

    // 4. Deletar (POST para evitar deleções acidentais via link)
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            repo.deleteById(id);
            redirectAttributes.addFlashAttribute("sucesso", "Item removido da Shark!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não é possível excluir: existem vendas vinculadas a este item.");
        }
        return "redirect:/estoque";
    }
}