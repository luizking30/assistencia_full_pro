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

    // --- 🔐 SEGURANÇA SAAS: LISTAGEM POR LOJA ---
    List<Venda> findByEmpresaIdOrderByDataHoraDesc(Long empresaId);

    // --- 🚀 MÉTODOS PARA O DASHBOARD (RESOLVE O ERRO DE COMPILAÇÃO) ---

    // O Dashboard agora pede este método com EmpresaId:
    long countByEmpresaIdAndDataHoraBetween(Long empresaId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0.0) FROM Venda v " +
            "WHERE v.empresa.id = :empresaId AND v.dataHora BETWEEN :inicio AND :fim")
    Double somarVendasDoDia(@Param("empresaId") Long empresaId,
                            @Param("inicio") LocalDateTime inicio,
                            @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.custoTotalEstoque), 0.0) FROM Venda v " +
            "WHERE v.empresa.id = :empresaId AND v.dataHora BETWEEN :inicio AND :fim")
    Double somarCustoEstoqueDasVendasDoDia(@Param("empresaId") Long empresaId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);

    // --- SISTEMA DE COMISSÃO (FILTRADO POR EMPRESA) ---
    List<Venda> findByEmpresaIdAndVendedorIdAndPagoFalse(Long empresaId, Long vendedorId);

    @Query("SELECT COALESCE(SUM(v.valorTotal), 0.0) FROM Venda v " +
            "WHERE v.empresa.id = :empresaId AND v.vendedor.id = :vendedorId AND v.pago = false")
    Double somarTotalVendasPendentesPorVendedor(@Param("empresaId") Long empresaId, @Param("vendedorId") Long vendedorId);

    // --- BUSCAS DINÂMICAS E AJAX (FILTRADO POR EMPRESA) ---

    List<Venda> findByEmpresaIdAndVendedorNomeContainingIgnoreCaseAndDataHoraBetween(Long empresaId, String nome, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT DISTINCT v FROM Venda v JOIN v.itens i " +
            "WHERE v.empresa.id = :empresaId AND i.produto.nome LIKE %:nome%")
    List<Venda> findByNomeProduto(@Param("empresaId") Long empresaId, @Param("nome") String nome);

    List<Venda> findByEmpresaIdAndDataHoraBetween(Long empresaId, LocalDateTime inicio, LocalDateTime fim);
}