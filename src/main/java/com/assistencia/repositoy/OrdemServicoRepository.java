package com.assistencia.repository;

import com.assistencia.model.OrdemServico;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    @Override
    default List<OrdemServico> findAll() {
        return findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Query("SELECT os FROM OrdemServico os WHERE " +
            "LOWER(CONCAT(os.id, '')) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(os.clienteNome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "os.clienteWhatsapp LIKE CONCAT('%', :termo, '%') OR " +
            "LOWER(os.produto) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(os.status) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "ORDER BY os.id DESC")
    List<OrdemServico> buscarSugestoesSugestivas(@Param("termo") String termo);

    // --- 🚀 MÉTODOS CORRIGIDOS PARA O DASHBOARD ---

    // Conta OS criadas hoje (Data de abertura)
    long countByDataBetween(LocalDateTime inicio, LocalDateTime fim);

    // Conta OS entregues hoje (IMPORTANTE: Agora usa o campo 'dataEntrega')
    long countByStatusAndDataEntregaBetween(String status, LocalDateTime inicio, LocalDateTime fim);

    // Soma o Valor Bruto das OS entregues hoje
    @Query("SELECT COALESCE(SUM(os.valorTotal), 0.0) FROM OrdemServico os " +
            "WHERE os.status = :status " +
            "AND os.dataEntrega BETWEEN :inicio AND :fim")
    Double somarValorBrutoOsEntregues(@Param("status") String status,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    // NOVO: Soma o Custo de Peças das OS entregues hoje
    @Query("SELECT COALESCE(SUM(os.custoPeca), 0.0) FROM OrdemServico os " +
            "WHERE os.status = :status " +
            "AND os.dataEntrega BETWEEN :inicio AND :fim")
    Double somarCustoPecasOsEntregues(@Param("status") String status,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    // --- OUTRAS BUSCAS ---
    List<OrdemServico> findByStatusAndDataEntregaBetweenOrderByIdDesc(String status, LocalDateTime inicio, LocalDateTime fim);
    List<OrdemServico> findByStatusOrderByIdDesc(String status);
    List<OrdemServico> findByClienteNomeContainingIgnoreCaseOrderByIdDesc(String nome);
}