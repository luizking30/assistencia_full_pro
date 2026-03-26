package com.assistencia.repository;

import com.assistencia.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    // Retorna a LISTA de vendas para preencher as tabelas do histórico/relatório
    List<Venda> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    // Conta a quantidade de vendas (Para o card "Vendas Feitas")
    long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    // SOMA o valor total das vendas do dia (Para o card "Total em Vendas")
    // O COALESCE garante que, se não houver vendas, o retorno seja 0.0 em vez de null
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0.0) FROM Venda v WHERE v.dataHora BETWEEN :inicio AND :fim")
    Double somarVendasDoDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // OPCIONAL: Busca vendas de um cliente específico pelo nome (útil para filtros futuros)
    @Query("SELECT v FROM Venda v JOIN v.itens i WHERE i.produto.nome LIKE %:nome%")
    List<Venda> findByNomeProduto(@Param("nome") String nome);
}