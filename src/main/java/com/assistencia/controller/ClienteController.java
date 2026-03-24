package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.repository.ClienteRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/clientes") // prefixo para todas as rotas de clientes
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/") // rota: /clientes/
    public String listar(Model model, HttpSession session) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "clientes"; // nome do template Thymeleaf
    }

    @GetMapping("/novo") // rota: /clientes/novo
    public String novoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cliente_form";
    }
}