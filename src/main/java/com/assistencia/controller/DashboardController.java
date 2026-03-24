package com.assistencia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    // 🔥 NOVO: rota raiz
    @GetMapping("/")
    public String home() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "dashboard"; // Página principal do admin
    }

    @GetMapping("/admin/clientes")
    public String listarClientesAdmin(Model model) {
        // lógica para listar clientes para admin
        return "clientes_admin";
    }
}