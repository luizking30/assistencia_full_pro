package com.assistencia.repository;

import com.assistencia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Busca clientes cujo nome contém o termo (ignora maiúsculas/minúsculas)
    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    // Busca clientes cujo CPF contém o termo
    List<Cliente> findByCpfContaining(String cpf);

    // Busca clientes cujo WhatsApp contém o termo
    List<Cliente> findByWhatsappContaining(String whatsapp);
}