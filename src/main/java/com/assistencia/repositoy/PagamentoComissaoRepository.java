package com.assistencia.repository;

import com.assistencia.model.PagamentoComissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PagamentoComissaoRepository extends JpaRepository<PagamentoComissao, Long> {

    // Soma histórica de pagamentos para um funcionário específico (Total Geral)
    @Query("SELECT COALESCE(SUM(p.valorPago), 0.0) FROM PagamentoComissao p WHERE p.funcionarioId = :id")
    Double somarTotalPagoAoFuncionario(@Param("id") Long id);

    /**
     * NOVO MÉTODO: Soma pagamentos filtrando por tipo (OS ou VENDA).
     * Essencial para que o abatimento ocorra na coluna separada na Shark Eletrônicos.
     */
    @Query("SELECT COALESCE(SUM(p.valorPago), 0.0) FROM PagamentoComissao p " +
            "WHERE p.funcionarioId = :id AND p.tipoComissao = :tipo")
    Double somarPorFuncionarioETipo(@Param("id") Long id, @Param("tipo") String tipo);

    // Busca os últimos 10 pagamentos ordenados por data decrescente
    List<PagamentoComissao> findTop10ByOrderByDataHoraDesc();
}