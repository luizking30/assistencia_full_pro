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

    // --- NOVOS CAMPOS PARA CONGELAR A COMISSÃO ---
    @Column(name = "comissao_vendedor_valor")
    private Double comissaoVendedorValor = 0.0; // Valor em R$ ja calculado

    @Column(name = "taxa_comissao_aplicada")
    private Double taxaComissaoAplicada = 0.0; // A % que ele tinha no ato da venda
    // --------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario vendedor;

    @Column(name = "nome_vendedor_no_ato")
    private String nomeVendedorNoAto;

    private boolean pago = false;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda() {
        this.dataHora = LocalDateTime.now();
    }

    /**
     * Sincroniza cálculos, estoque e congela comissões antes de persistir.
     */
    @PrePersist
    @PreUpdate
    public void validarDadosAntesDeSalvar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        // 1. Registra quem vendeu e qual era a taxa dele HOJE
        if (this.vendedor != null) {
            this.nomeVendedorNoAto = this.vendedor.getNome();

            // Só define a taxa se for uma venda nova (taxa ainda é 0)
            // ou se você quiser permitir reprocessamento manual.
            if (this.taxaComissaoAplicada == null || this.taxaComissaoAplicada == 0.0) {
                this.taxaComissaoAplicada = (this.vendedor.getComissaoVenda() != null)
                        ? this.vendedor.getComissaoVenda() : 0.0;
            }
        } else if (this.nomeVendedorNoAto == null) {
            this.nomeVendedorNoAto = "Sistema Shark";
        }

        double totalVendaCalculado = 0.0;
        double totalCustoCalculado = 0.0;

        if (itens != null) {
            for (ItemVenda item : itens) {
                item.setVenda(this);

                int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;
                double precoVenda = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
                double precoCusto = (item.getCustoUnitario() != null) ? item.getCustoUnitario() : 0.0;

                totalVendaCalculado += (precoVenda * qtd);
                totalCustoCalculado += (precoCusto * qtd);
            }
        }

        this.valorTotal = totalVendaCalculado;
        this.custoTotalEstoque = totalCustoCalculado;

        // 2. CONGELA O VALOR EM DINHEIRO DA COMISSÃO
        // Mesmo que o admin mude a % do funcionário depois, este valor aqui não muda mais.
        if (this.taxaComissaoAplicada > 0) {
            this.comissaoVendedorValor = (this.valorTotal * this.taxaComissaoAplicada) / 100;
        } else {
            this.comissaoVendedorValor = 0.0;
        }
    }
}