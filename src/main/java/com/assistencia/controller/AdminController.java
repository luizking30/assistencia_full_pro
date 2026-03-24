package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
import com.assistencia.model.Venda;
import com.assistencia.model.Usuario;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.VendaRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private OrdemServicoRepository ordemRepo;

    @Autowired
    private VendaRepository vendaRepo;

    private boolean isAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        return usuario != null && "admin".equals(usuario.getRole());
    }

    // Deletar Cliente
    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/acesso-negado";
        clienteRepo.deleteById(id);
        return "redirect:/clientes";
    }

    // Deletar Ordem de Serviço
    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/acesso-negado";
        ordemRepo.deleteById(id);
        return "redirect:/ordens";
    }

    // Deletar Venda
    @PostMapping("/venda/deletar/{id}")
    public String deletarVenda(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/acesso-negado";
        vendaRepo.deleteById(id);
        return "redirect:/vendas";
    }
}