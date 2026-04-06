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

    @Column(name = "comissao_vendedor_valor")
    private Double comissaoVendedorValor = 0.0;

    @Column(name = "taxa_comissao_aplicada")
    private Double taxaComissaoAplicada = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

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
     * MÉTODO SHARK: Adiciona um item e sincroniza a Empresa automaticamente.
     * Isso evita o erro de 'empresa_id' null nos itens da venda.
     */
    public void adicionarItem(ItemVenda item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        this.itens.add(item);
        item.setVenda(this);

        // Se a venda já tem empresa, o item herda na hora (DNA SaaS)
        if (this.empresa != null) {
            item.setEmpresa(this.empresa);
        }
    }

    @PrePersist
    @PreUpdate
    public void validarDadosAntesDeSalvar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        // Lógica de Vendedor e Nome no ato
        if (this.vendedor != null) {
            this.nomeVendedorNoAto = this.vendedor.getNome();
            if (this.taxaComissaoAplicada == null || this.taxaComissaoAplicada == 0.0) {
                this.taxaComissaoAplicada = (this.vendedor.getComissaoVenda() != null)
                        ? this.vendedor.getComissaoVenda() : 0.0;
            }
        } else if (this.nomeVendedorNoAto == null) {
            this.nomeVendedorNoAto = "Sistema Shark";
        }

        // Cálculos de totais baseados nos itens
        double totalVendaCalculado = 0.0;
        double totalCustoCalculado = 0.0;

        if (itens != null) {
            for (ItemVenda item : itens) {
                item.setVenda(this);
                // Garante que o item tenha a empresa da venda pai antes de salvar
                if (this.empresa != null) {
                    item.setEmpresa(this.empresa);
                }

                int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;
                double precoVenda = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
                double precoCusto = (item.getCustoUnitario() != null) ? item.getCustoUnitario() : 0.0;

                totalVendaCalculado += (precoVenda * qtd);
                totalCustoCalculado += (precoCusto * qtd);
            }
        }

        this.valorTotal = totalVendaCalculado;
        this.custoTotalEstoque = totalCustoCalculado;

        // Cálculo da Comissão do Vendedor
        if (this.taxaComissaoAplicada != null && this.taxaComissaoAplicada > 0) {
            this.comissaoVendedorValor = (this.valorTotal * this.taxaComissaoAplicada) / 100;
        } else {
            this.comissaoVendedorValor = 0.0;
        }
    }
}