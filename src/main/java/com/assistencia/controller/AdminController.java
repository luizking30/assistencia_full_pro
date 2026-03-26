package com.assistencia.controller;

import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.OrdemServicoRepository; // 🚀 ADICIONE ESTA LINHA OBRIGATORIAMENTE

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private OrdemServicoRepository ordemRepo; // ✅ Agora o símbolo será encontrado

    @Autowired
    private VendaRepository vendaRepo;

    // ✅ DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // 🧑‍💼 Deletar Cliente
    @PostMapping("/cliente/deletar/{id}")
    public String deletarCliente(@PathVariable Long id) {
        clienteRepo.deleteById(id);
        return "redirect:/clientes";
    }

    // 🔧 Deletar Ordem
    @PostMapping("/ordem/deletar/{id}")
    public String deletarOrdem(@PathVariable Long id) {
        ordemRepo.deleteById(id);
        return "redirect:/ordens";
    }

    // 💰 Deletar Venda
    @PostMapping("/venda/deletar/{id}")
    public String deletarVenda(@PathVariable Long id) {
        vendaRepo.deleteById(id);
        return "redirect:/vendas";
    }
}