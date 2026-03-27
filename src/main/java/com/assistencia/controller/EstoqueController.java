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
import java.util.Optional;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final ProdutoRepository repo;

    public EstoqueController(ProdutoRepository repo) {
        this.repo = repo;
    }

    // 1. Listar (Ordenado por ID para manter a organização na Shark)
    @GetMapping
    public String listar(Model model) {
        List<Produto> produtos = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("produtos", produtos);

        // Se não houver mensagem de erro/sucesso vindo de um redirect, garante que o objeto esteja pronto
        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        return "estoque";
    }

    // 2. Buscar por ID (Essencial para o seu JavaScript de "verificarDuplicidade" e "editar")
    @GetMapping("/buscar/{id}")
    @ResponseBody
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Salvar ou Atualizar (Com trava de segurança para o ID Manual)
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Produto produto, RedirectAttributes redirectAttributes) {
        try {
            if (produto.getId() == null) {
                redirectAttributes.addFlashAttribute("mensagemErro", "O ID do produto é obrigatório para o controle Shark!");
                return "redirect:/estoque";
            }

            // Lógica de segurança: Se for um NOVO cadastro, verifica se o ID já existe no banco
            // Se o ID já existe e não estamos editando (ou seja, o usuário digitou um ID ocupado),
            // o Spring Data JPA 'save' iria sobrescrever. Aqui tratamos como atualização.

            boolean jaExiste = repo.existsById(produto.getId());
            repo.save(produto);

            String mensagem = jaExiste ? "Produto atualizado com sucesso!" : "Novo produto cadastrado com sucesso!";
            redirectAttributes.addFlashAttribute("sucesso", mensagem);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro técnico ao processar: " + e.getMessage());
        }
        return "redirect:/estoque";
    }

    // 4. Deletar
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (repo.existsById(id)) {
                repo.deleteById(id);
                redirectAttributes.addFlashAttribute("sucesso", "Item removido com sucesso do estoque!");
            } else {
                redirectAttributes.addFlashAttribute("mensagemErro", "Produto não encontrado.");
            }
        } catch (Exception e) {
            // Caso o produto esteja vinculado a uma Venda ou Ordem de Serviço (Foreign Key)
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível excluir: este item possui movimentações vinculadas.");
        }
        return "redirect:/estoque";
    }

    // 🔥 Sugestões para o Autocomplete da Venda (Para uso futuro)
    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Produto> buscarSugestoes(@RequestParam String termo) {
        return repo.findTop10ByNomeContainingIgnoreCase(termo);
    }
}