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

    // --- 🚀 ORDENAÇÃO PADRÃO (SOBRESCREVENDO O FIND ALL) ---
    // Isso garante que ao chamar repository.findAll(), venha do maior ID para o menor
    @Override
    default List<OrdemServico> findAll() {
        return findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // --- 🔍 BUSCA UNIFICADA (ESTILO GOOGLE) COM ORDENAÇÃO ---
    @Query("SELECT os FROM OrdemServico os WHERE " +
            "LOWER(CONCAT(os.id, '')) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(os.clienteNome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "os.clienteWhatsapp LIKE CONCAT('%', :termo, '%') OR " +
            "LOWER(os.produto) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(os.status) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "ORDER BY os.id DESC") // Adicionado Order By
    List<OrdemServico> buscarSugestoesSugestivas(@Param("termo") String termo);

    // --- BUSCAS PARA RELATÓRIOS E HISTÓRICO ---
    List<OrdemServico> findByStatusAndDataEntregaBetweenOrderByIdDesc(String status, LocalDateTime inicio, LocalDateTime fim);
    List<OrdemServico> findByStatusOrderByIdDesc(String status);

    // --- BUSCAS ESPECÍFICAS ---
    List<OrdemServico> findByClienteNomeContainingIgnoreCaseOrderByIdDesc(String nome);
    List<OrdemServico> findByClienteWhatsappContainingOrderByIdDesc(String whatsapp);
    List<OrdemServico> findByProdutoContainingIgnoreCaseOrderByIdDesc(String produto);

    // --- CONTADORES PARA O DASHBOARD (Permanecem iguais) ---
    long countByDataBetween(LocalDateTime inicio, LocalDateTime fim);
    long countByStatusAndDataBetween(String status, LocalDateTime inicio, LocalDateTime fim);

    // --- FINANCEIRO (DASHBOARD) ---
    @Query("SELECT COALESCE(SUM(os.valorTotal), 0.0) FROM OrdemServico os " +
            "WHERE os.status = :status " +
            "AND os.dataEntrega BETWEEN :inicio AND :fim")
    Double somarValorServicosDoDia(@Param("status") String status,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim);
}