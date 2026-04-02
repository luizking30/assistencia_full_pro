package com.assistencia.repository;

import com.assistencia.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {

    // 1. Busca padrão para o Dashboard (Ordenado por Vencimento)
    List<Conta> findAllByOrderByDataVencimentoAsc();

    // 2. Busca específica para o Mês Atual (Usado no filtro do Controller)
    List<Conta> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);

    // 3. Busca apenas o que já foi PAGO (Para o Histórico debaixo)
    List<Conta> findByPagoTrueOrderByDataVencimentoDesc();

    // --- QUERIES DE SOMA (Opcionais, pois estamos somando via Stream no Controller) ---

    @Query("SELECT SUM(c.valor) FROM Conta c WHERE c.pago = true")
    Double somarTotalPagoGeral();

    // Soma apenas o que não está pago dentro de um período específico
    @Query("SELECT SUM(c.valor) FROM Conta c WHERE c.pago = false AND c.dataVencimento BETWEEN :inicio AND :fim")
    Double somarPendenteDoMes(LocalDate inicio, LocalDate fim);
}