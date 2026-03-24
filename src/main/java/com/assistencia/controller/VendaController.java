package com.assistencia.controller;

import com.assistencia.model.Venda;
import com.assistencia.repository.VendaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/vendas")
public class VendaController {

    private final VendaRepository repo;

    public VendaController(VendaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("vendas", repo.findAll());
        return "vendas";
    }

    @PostMapping
    public String salvar(Venda v) {
        v.setDataHora(LocalDateTime.now());
        repo.save(v);
        return "redirect:/vendas";
    }
}