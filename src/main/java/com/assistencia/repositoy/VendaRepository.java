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

    // --- RELATÓRIOS E DASHBOARD ---
    List<Venda> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
    long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0.0) FROM Venda v WHERE v.dataHora BETWEEN :inicio AND :fim")
    Double somarVendasDoDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.custoTotalEstoque), 0.0) FROM Venda v WHERE v.dataHora BETWEEN :inicio AND :fim")
    Double somarCustoEstoqueDasVendasDoDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // --- SISTEMA DE COMISSÃO ---
    List<Venda> findByVendedorIdAndPagoFalse(Long vendedorId);

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0.0) FROM Venda v WHERE v.vendedor.id = :vendedorId AND v.pago = false")
    Double somarTotalVendasPendentesPorVendedor(@Param("vendedorId") Long vendedorId);

    // --- NOVOS MÉTODOS PARA A BUSCA DINÂMICA (AJAX) ---

    /**
     * Busca por nome do vendedor (ignora maiúsculas/minúsculas) e período de tempo.
     */
    List<Venda> findByVendedorNomeContainingIgnoreCaseAndDataHoraBetween(String nome, LocalDateTime inicio, LocalDateTime fim);

    /**
     * Busca por produtos (Já existente no seu código)
     */
    @Query("SELECT DISTINCT v FROM Venda v JOIN v.itens i WHERE i.produto.nome LIKE %:nome%")
    List<Venda> findByNomeProduto(@Param("nome") String nome);
}