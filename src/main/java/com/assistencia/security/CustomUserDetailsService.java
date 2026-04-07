package com.assistencia.security;

import com.assistencia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException; // IMPORTANTE: Adicione este import
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.assistencia.model.Usuario;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Busca no MySQL
        Usuario usuario = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        // 2. Trava de segurança para aprovação (AJUSTADO)
        if (!usuario.isAprovado()) {
            // Trocamos para DisabledException para o SecurityConfig capturar o motivo real do bloqueio
            throw new DisabledException("Usuário " + username + " ainda não foi aprovado pelo administrador.");
        }

        // 3. Retorno formatado
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(usuario.getRole())
                .build();
    }
}