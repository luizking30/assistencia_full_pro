package com.assistencia.repository;

import com.assistencia.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // Importante adicionar este import

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 🔥 ESTA LINHA RESOLVE O ERRO "cannot find symbol"
    // O Spring transformará isso em: SELECT * FROM produto WHERE nome LIKE %termo% LIMIT 10
    List<Produto> findTop10ByNomeContainingIgnoreCase(String nome);

    // Opcional: Se quiser buscar por ID ou Nome na mesma caixa futuramente:
    // List<Produto> findByIdOrNomeContainingIgnoreCase(Long id, String nome);
}