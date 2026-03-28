package com.assistencia.security;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Busca no MySQL
        Usuario usuario = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        // 2. Trava de segurança para aprovação
        if (!usuario.isAprovado()) {
            // Importante: O Spring Security captura essa exceção para exibir mensagens no login
            throw new UsernameNotFoundException("Usuário " + username + " ainda não foi aprovado pelo administrador.");
        }

        // 3. Retorno formatado
        // Usamos .authorities() em vez de .roles() porque no seu banco
        // a permissão já está salva com o prefixo completo "ROLE_ADMIN".
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(usuario.getRole()) // Passa "ROLE_ADMIN" ou "ROLE_FUNCIONARIO" direto
                .build();
    }
}