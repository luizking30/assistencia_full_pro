package com.assistencia.controller;

import com.assistencia.model.Produto;
import com.assistencia.model.Usuario;
import com.assistencia.repository.ProdutoRepository;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final ProdutoRepository repo;
    private final UsuarioRepository usuarioRepo;

    @Autowired
    public EstoqueController(ProdutoRepository repo, UsuarioRepository usuarioRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
    }

    // 1. Listar (Filtrado por Empresa para manter o isolamento SaaS)
    @GetMapping
    public String listar(Model model) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return "redirect:/login";

        // Busca apenas produtos da empresa do usuário logado
        List<Produto> produtos = repo.findByEmpresaId(logado.getEmpresa().getId());
        model.addAttribute("produtos", produtos);

        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        return "estoque";
    }

    // 2. Buscar por ID (Usado no JS para carregar dados na edição clássica)
    @GetMapping("/buscar/{id}")
    @ResponseBody
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        Usuario logado = getUsuarioLogado();
        return repo.findById(id)
                .filter(p -> p.getEmpresa().getId().equals(logado.getEmpresa().getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔥 NOVO: Buscar por Código de Barras (Usado para o modo de edição automática via onchange)
    @GetMapping("/buscar-por-codigo")
    @ResponseBody
    public ResponseEntity<Produto> buscarPorCodigo(@RequestParam String codigo) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return ResponseEntity.status(401).build();

        // Busca o código garantindo o isolamento da empresa (SaaS)
        return repo.findByCodigoBarrasAndEmpresaId(codigo, logado.getEmpresa().getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Salvar ou Atualizar
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Produto produto, RedirectAttributes redirectAttributes) {
        try {
            Usuario logado = getUsuarioLogado();
            if (logado == null || logado.getEmpresa() == null) {
                redirectAttributes.addFlashAttribute("mensagemErro", "Sessão inválida!");
                return "redirect:/login";
            }

            // 🔐 VINCULA SEMPRE À EMPRESA LOGADA
            produto.setEmpresa(logado.getEmpresa());

            // Validação amigável de Código de Barras duplicado na mesma loja
            if (produto.getCodigoBarras() != null && !produto.getCodigoBarras().isEmpty()) {
                Optional<Produto> existenteCod = repo.findByCodigoBarrasAndEmpresaId(produto.getCodigoBarras(), logado.getEmpresa().getId());
                if (existenteCod.isPresent()) {
                    if (produto.getId() == null || !existenteCod.get().getId().equals(produto.getId())) {
                        redirectAttributes.addFlashAttribute("mensagemErro", "O código '" + produto.getCodigoBarras() + "' já está em uso nesta loja.");
                        return "redirect:/estoque";
                    }
                }
            }

            // Se o ID vier preenchido, validamos se o item pertence à empresa antes de salvar
            if (produto.getId() != null) {
                Produto prodBanco = repo.findById(produto.getId()).orElse(null);
                if (prodBanco != null && !prodBanco.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                    redirectAttributes.addFlashAttribute("mensagemErro", "Operação não permitida.");
                    return "redirect:/estoque";
                }
            }

            repo.save(produto);
            redirectAttributes.addFlashAttribute("sucesso", "Produto processado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/estoque";
    }

    // 4. Deletar
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Usuario logado = getUsuarioLogado();
            Produto p = repo.findById(id).orElse(null);

            if (p != null && p.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                repo.delete(p);
                redirectAttributes.addFlashAttribute("sucesso", "Item removido com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("mensagemErro", "Acesso negado ou produto inexistente.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível excluir: item com movimentações ativas.");
        }
        return "redirect:/estoque";
    }

    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Produto> buscarSugestoes(@RequestParam String termo) {
        Usuario logado = getUsuarioLogado();
        return repo.findByNomeContainingIgnoreCaseAndEmpresaId(termo, logado.getEmpresa().getId());
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepo.findByUsername(login).orElse(null);
    }
}