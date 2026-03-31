package com.assistencia.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venda")
@Data
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal = 0.0;

    @Column(name = "custo_total_estoque")
    private Double custoTotalEstoque = 0.0;

    // MELHORIA: nullable = true permite que o JPA salve mesmo se o vendedor não for setado no objeto,
    // evitando o erro de "default value" no MySQL se você ajustar a coluna no banco também.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = true)
    @ToString.Exclude // Evita erro de recursão no log do Lombok
    @EqualsAndHashCode.Exclude
    private Usuario vendedor;

    @Column(name = "nome_vendedor_no_ato")
    private String nomeVendedorNoAto;

    private boolean pago = false;

    // MELHORIA: Adicionado orphanRemoval para limpar itens excluídos da lista
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda() {
        this.dataHora = LocalDateTime.now();
    }

    /**
     * Sincroniza cálculos e histórico antes de persistir.
     */
    @PrePersist
    @PreUpdate
    public void validarDadosAntesDeSalvar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        // Garante o registro do nome para auditoria (Shark Eletrônicos)
        if (this.vendedor != null) {
            this.nomeVendedorNoAto = this.vendedor.getNome();
        } else if (this.nomeVendedorNoAto == null) {
            this.nomeVendedorNoAto = "Sistema Shark";
        }

        double totalVendaCalculado = 0.0;
        double totalCustoCalculado = 0.0;

        if (itens != null) {
            for (ItemVenda item : itens) {
                item.setVenda(this); // Sincronização bidirecional essencial

                int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;
                double precoVenda = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
                double precoCusto = (item.getCustoUnitario() != null) ? item.getCustoUnitario() : 0.0;

                totalVendaCalculado += (precoVenda * qtd);
                totalCustoCalculado += (precoCusto * qtd);
            }
        }

        this.valorTotal = totalVendaCalculado;
        this.custoTotalEstoque = totalCustoCalculado;
    }
}