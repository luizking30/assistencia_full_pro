package com.assistencia.repository;

import com.assistencia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * 🔑 RECUPERAÇÃO DE SENHA:
     * Busca o usuário pelo token único gerado no e-mail.
     */
    Optional<Usuario> findByResetPasswordToken(String token);

    /**
     * 🔍 BUSCA TRIPLA SHARK (Username, E-mail ou WhatsApp):
     * O Spring filtrará o banco por qualquer um desses 3 campos.
     */
    Optional<Usuario> findByUsernameOrEmailOrWhatsapp(String username, String email, String whatsapp);

    /**
     * 1. ESSENCIAL: Busca pelo nome de usuário (Usado pelo Spring Security).
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * 📧 VALIDAÇÃO: Busca por e-mail para verificar duplicidade no cadastro.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * 📱 VALIDAÇÃO WHATSAPP: Busca para verificar duplicidade de número.
     */
    Optional<Usuario> findByWhatsapp(String whatsapp);

    /**
     * ⚡ PERFORMANCE: Verifica se existe sem trazer o objeto inteiro do banco.
     */
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByWhatsapp(String whatsapp);

    /**
     * 🔐 SEGURANÇA SaaS: Lista apenas os funcionários da loja logada.
     */
    List<Usuario> findByEmpresaId(Long empresaId);

    /**
     * 🔐 SEGURANÇA SaaS: Busca quem aguarda aprovação APENAS na sua empresa.
     */
    List<Usuario> findByEmpresaIdAndAprovadoFalse(Long empresaId);

    /**
     * 3. ÚTIL: Busca todos os aprovados de uma empresa específica.
     */
    List<Usuario> findByEmpresaIdAndAprovadoTrue(Long empresaId);

    /**
     * Busca global de usuários não aprovados.
     */
    List<Usuario> findByAprovadoFalse();
}