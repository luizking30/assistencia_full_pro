package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.repository.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository repo;

    public ClienteController(ClienteRepository repo) {
        this.repo = repo;
    }

    /**
     * ENDPOINT PARA AUTOCOMPLETE (ESTILO GOOGLE)
     * Retorna JSON com ID, Nome e WhatsApp para o formulário de O.S.
     */
    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Map<String, Object>> buscarSugestoes(@RequestParam String termo) {
        // Busca clientes que contêm o termo no nome (ignora maiúsculas/minúsculas)
        List<Cliente> clientes = repo.findByNomeContainingIgnoreCase(termo);

        // Converte a lista de Clientes em uma lista de Mapas simples (JSON)
        return clientes.stream()
                .limit(8) // Limita a 8 resultados para manter a performance "Shark"
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("nome", c.getNome());
                    map.put("whatsapp", c.getWhatsapp());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", repo.findAll());
        return "clientes";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Cliente c, RedirectAttributes attributes) {
        Optional<Cliente> clienteExistente = repo.findByCpf(c.getCpf());

        if (clienteExistente.isPresent()) {
            if (c.getId() == null || !clienteExistente.get().getId().equals(c.getId())) {
                attributes.addFlashAttribute("mensagemErro",
                        "Atenção: O CPF " + c.getCpf() + " já pertence ao cliente " + clienteExistente.get().getNome());
                return "redirect:/clientes";
            }
        }

        try {
            repo.save(c);
            attributes.addFlashAttribute("mensagemSucesso", "Dados do cliente salvos com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro técnico ao salvar.");
        }

        return "redirect:/clientes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            if (repo.existsById(id)) {
                repo.deleteById(id);
                attributes.addFlashAttribute("mensagemSucesso", "Cliente removido com sucesso.");
            }
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir este cliente pois existem Ordens de Serviço vinculadas.");
        }
        return "redirect:/clientes";
    }
}