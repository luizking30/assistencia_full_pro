package com.assistencia.repository;

import com.assistencia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Agora o nome do método casa com a variável 'username' da Model
    Optional<Usuario> findByUsername(String username);

    // Caso você use login manual em algum lugar
    Usuario findByUsernameAndSenha(String username, String senha);
}