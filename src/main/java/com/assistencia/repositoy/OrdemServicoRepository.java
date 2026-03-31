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

    // --- 🚀 MÉTODOS PARA O DASHBOARD E RELATÓRIOS ---

    long countByDataBetween(LocalDateTime inicio, LocalDateTime fim);

    long countByStatusAndDataEntregaBetween(String status, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(os.valorTotal), 0.0) FROM OrdemServico os " +
            "WHERE LOWER(os.status) = LOWER(:status) " +
            "AND os.dataEntrega BETWEEN :inicio AND :fim")
    Double somarValorBrutoOsEntregues(@Param("status") String status,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(os.custoPeca), 0.0) FROM OrdemServico os " +
            "WHERE LOWER(os.status) = LOWER(:status) " +
            "AND os.dataEntrega BETWEEN :inicio AND :fim")
    Double somarCustoPecasOsEntregues(@Param("status") String status,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    // --- 🛠️ NOVOS MÉTODOS PARA COMISSÃO E PAGAMENTO ---

    /**
     * Busca todas as Ordens de Serviço de um técnico específico que:
     * 1. Estão com status 'ENTREGUE' ou 'CONCLUIDO'
     * 2. Ainda não foram marcadas como pagas (pago = false)
     */
    List<OrdemServico> findByTecnicoIdAndPagoFalse(Long tecnicoId);

    /**
     * Soma o valor total (Mão de obra + Peças) das OS pendentes de um técnico
     */
    @Query("SELECT COALESCE(SUM(os.valorTotal), 0.0) FROM OrdemServico os WHERE os.tecnico.id = :tecnicoId AND os.pago = false")
    Double somarTotalOsPendentesPorTecnico(@Param("tecnicoId") Long tecnicoId);

    // --- MÉTODOS DE BUSCA PARA LISTAGEM ---

    List<OrdemServico> findByStatusAndDataEntregaBetween(String status, LocalDateTime inicio, LocalDateTime fim);

    List<OrdemServico> findByStatusOrderByIdDesc(String status);

    List<OrdemServico> findByClienteNomeContainingIgnoreCaseOrderByIdDesc(String nome);
}