package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.Empresa;
import com.assistencia.model.Usuario;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Autowired
    private ClienteRepository repo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    /**
     * ENDPOINT PARA AUTOCOMPLETE (ESTILO GOOGLE)
     */
    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Map<String, Object>> buscarSugestoes(@RequestParam String termo) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return new java.util.ArrayList<>();

        List<Cliente> clientes = repo.findByNomeContainingIgnoreCaseAndEmpresaId(termo, logado.getEmpresa().getId());

        return clientes.stream()
                .limit(8)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("nome", c.getNome());
                    map.put("whatsapp", c.getWhatsapp());
                    map.put("cpf", c.getCpf());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping
    public String listar(Model model) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return "redirect:/login";

        List<Cliente> clientesDaEmpresa = repo.findByEmpresaId(logado.getEmpresa().getId());
        model.addAttribute("clientes", clientesDaEmpresa);
        return "clientes";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Cliente c, RedirectAttributes attributes) {
        try {
            Usuario logado = getUsuarioLogado();
            if (logado == null || logado.getEmpresa() == null) {
                attributes.addFlashAttribute("mensagemErro", "Sua sessão expirou. Por favor, faça login novamente.");
                return "redirect:/login";
            }

            c.setEmpresa(logado.getEmpresa());

            // --- VALIDAÇÃO AMIGÁVEL DE CPF DUPLICADO NA MESMA LOJA ---
            // Usamos o método que filtra por Empresa para permitir o mesmo CPF em lojas diferentes
            Optional<Cliente> clienteExistente = repo.findByCpfAndEmpresaId(c.getCpf(), logado.getEmpresa().getId());

            if (clienteExistente.isPresent()) {
                Cliente existente = clienteExistente.get();
                // Se for um novo cadastro (id null) ou se o ID encontrado for diferente do que estamos editando
                if (c.getId() == null || !existente.getId().equals(c.getId())) {
                    attributes.addFlashAttribute("mensagemErro",
                            "Opa! Já existe um cliente cadastrado com o CPF " + c.getCpf() + " (" + existente.getNome() + ").");
                    return "redirect:/clientes";
                }
            }

            repo.save(c);
            attributes.addFlashAttribute("mensagemSucesso", "Cliente " + c.getNome() + " salvo com sucesso!");

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Caso a validação do Java falhe e chegue no banco (Constraint do MySQL)
            attributes.addFlashAttribute("mensagemErro", "Este CPF já está em uso nesta unidade.");
        } catch (Exception e) {
            e.printStackTrace();
            attributes.addFlashAttribute("mensagemErro", "Não conseguimos salvar os dados agora. Tente novamente em instantes.");
        }

        return "redirect:/clientes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            Usuario logado = getUsuarioLogado();
            Optional<Cliente> clienteOpt = repo.findById(id);

            if (clienteOpt.isPresent() && clienteOpt.get().getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                repo.deleteById(id);
                attributes.addFlashAttribute("mensagemSucesso", "O cadastro do cliente foi removido.");
            } else {
                attributes.addFlashAttribute("mensagemErro", "Você não tem permissão para remover este registro.");
            }
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Este cliente possui históricos de O.S. ou Vendas e não pode ser excluído.");
        }
        return "redirect:/clientes";
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login;
        if (principal instanceof UserDetails) {
            login = ((UserDetails) principal).getUsername();
        } else {
            login = principal.toString();
        }
        return usuarioRepo.findByUsername(login).orElse(null);
    }
}