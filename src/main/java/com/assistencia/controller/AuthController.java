package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Abre a página de login/registro
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // 2. Processa o novo cadastro (Solicitação de Acesso)
    @PostMapping("/registro")
    public String registrar(Usuario usuario, RedirectAttributes attributes) {
        try {
            // Criptografa a senha para o padrão BCrypt (segurança)
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

            // Define o papel inicial e trava o acesso (Aprovação pendente)
            usuario.setRole("ROLE_FUNCIONARIO");
            usuario.setAprovado(false);

            usuarioRepository.save(usuario);

            // Redireciona com o parâmetro de sucesso para o HTML mostrar o alerta
            attributes.addAttribute("success", true);

        } catch (Exception e) {
            // Se der erro (ex: usuário já existe), volta com alerta de erro
            attributes.addAttribute("error", true);
        }

        return "redirect:/login";
    }
}