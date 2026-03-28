package com.assistencia.repository;

import com.assistencia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 1. ESSENCIAL: Busca pelo nome de usuário (Usado pelo Spring Security)
    Optional<Usuario> findByUsername(String username);

    // 2. ÚTIL: Para sua tela de Gestão, buscar quem ainda não foi aprovado
    List<Usuario> findByAprovadoFalse();

    // NOTA: O método findByUsernameAndSenha foi REMOVIDO.
    // O Spring Security cuida da senha usando BCrypt automaticamente.
}