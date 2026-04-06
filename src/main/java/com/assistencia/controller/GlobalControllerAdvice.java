package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UsuarioRepository usuarioRepo;

    public GlobalControllerAdvice(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Blindagem 1: Verifica se auth é nulo, se não está autenticado ou se é usuário anônimo
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        try {
            // Blindagem 2: Busca sempre do banco para evitar dados "sujos" da sessão
            return usuarioRepo.findByUsername(auth.getName()).orElse(null);
        } catch (Exception e) {
            // Se houver erro de banco durante um erro 404, retorna null para não travar o layout
            return null;
        }
    }

    @ModelAttribute("isProprietario")
    public boolean isProprietario() {
        try {
            Usuario u = getUsuarioLogado();
            return u != null && u.getTipoFuncionario() != null &&
                    "PROPRIETARIO".equalsIgnoreCase(u.getTipoFuncionario());
        } catch (Exception e) {
            return false;
        }
    }
}