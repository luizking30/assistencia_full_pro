package com.assistencia.repository;

import com.assistencia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data JPA vai criar automaticamente a query com base no nome do método
    Usuario findByUsuarioAndSenha(String usuario, String senha);

}