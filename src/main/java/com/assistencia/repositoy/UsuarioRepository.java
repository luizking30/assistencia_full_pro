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

    // 🔐 SEGURANÇA SaaS: Lista apenas os funcionários da loja logada
    List<Usuario> findByEmpresaId(Long empresaId);

    // 🔐 SEGURANÇA SaaS: Busca quem aguarda aprovação APENAS na sua empresa
    List<Usuario> findByEmpresaIdAndAprovadoFalse(Long empresaId);

    // 3. ÚTIL: Busca todos os aprovados de uma empresa específica
    List<Usuario> findByEmpresaIdAndAprovadoTrue(Long empresaId);

    // Mantenha este apenas se for usar em algum dashboard global de SuperAdmin
    List<Usuario> findByAprovadoFalse();
}