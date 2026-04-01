package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
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

    /**
     * Este método cria a variável 'usuarioLogado' que o seu HTML (Thymeleaf)
     * está tentando ler. Sem isso, o HTML não encontra o "Tipo de Funcionário".
     */
    @ModelAttribute("usuarioLogado")
    public Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verifica se existe alguém logado e se não é um usuário anônimo
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            // Busca no banco de dados para pegar o campo 'tipoFuncionario'
            Optional<Usuario> usuario = usuarioRepo.findByUsername(auth.getName());
            return usuario.orElse(null);
        }
        return null;
    }
}