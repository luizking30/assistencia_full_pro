package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
// REMOVIDO: @RequestMapping("/admin") para bater com os links do seu menu
public class AdminController {

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private OrdemServicoRepository ordemRepo;

    @Autowired
    private VendaRepository vendaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    // --- DASHBOARD ---
    // REMOVIDO: Método dashboard() para evitar conflito com DashboardController

    // --- GESTÃO DE EQUIPE ---

    @GetMapping("/funcionarios")
    public String listarFuncionarios(Model model) {
        List<Usuario> todos = usuarioRepo.findAll();
        model.addAttribute("usuarios", todos);
        // AJUSTADO: Aponta para o arquivo funcionarios.html que está direto em templates
        return "funcionarios";
    }

    // ✅ Aprovar
    @PostMapping("/funcionarios/aprovar/{id}")
    public String aprovarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        Usuario usuario = usuarioRepo.findById(id).orElseThrow();
        usuario.setAprovado(true);
        usuarioRepo.save(usuario);
        ra.addFlashAttribute("mensagem", "Funcionário aprovado com sucesso!");
        return "redirect:/funcionarios";
    }

    // ❌ Deletar
    @PostMapping("/funcionarios/deletar/{id}")
    public String deletarFuncionario(@PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.deleteById(id);
        ra.addFlashAttribute("mensagem", "Acesso removido.");
        return "redirect:/funcionarios";
    }

    // --- EXCLUSÕES DE DADOS ---

    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id) {
        clienteRepo.deleteById(id);
        return "redirect:/clientes";
    }

    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id) {
        ordemRepo.deleteById(id);
        return "redirect:/ordens";
    }

    @PostMapping("/venda/deletar/{id}")
    public String deletarVenda(@PathVariable Long id) {
        vendaRepo.deleteById(id);
        return "redirect:/vendas";
    }
}