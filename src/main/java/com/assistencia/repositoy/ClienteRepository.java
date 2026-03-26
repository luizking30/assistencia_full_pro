package com.assistencia.repository;

import com.assistencia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // --- VALIDAÇÕES E BUSCAS EXATAS ---

    // Essencial para evitar duplicidade de cadastro
    Optional<Cliente> findByCpf(String cpf);

    // --- BUSCAS DINÂMICAS (PARA AJAX E FILTROS) ---

    // Busca enquanto digita na O.S. ou na tela de Clientes
    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    // Busca por parte do CPF
    List<Cliente> findByCpfContaining(String cpf);

    // Busca por parte do WhatsApp
    List<Cliente> findByWhatsappContaining(String whatsapp);

    // --- CONTADORES PARA O DASHBOARD (NOVO) ---

    /** * Conta quantos clientes foram cadastrados em um período específico.
     * Importante: Verifique se o campo na sua classe Cliente se chama 'dataCadastro'.
     */
    long countByDataCadastroBetween(LocalDateTime inicio, LocalDateTime fim);
}