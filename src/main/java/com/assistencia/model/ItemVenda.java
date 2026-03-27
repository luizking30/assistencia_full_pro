package com.assistencia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "item_venda")
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private Integer quantidade;

    private Double precoUnitario; // Preço FINAL (já com desconto) que o cliente pagou

    private Double desconto;      // PORCENTAGEM do desconto (ex: 5.0)

    // --- 🚀 CAMPO ADICIONADO PARA O DASHBOARD ---
    private Double custoUnitario = 0.0; // Valor de entrada do produto no estoque

    public ItemVenda() {}

    // --- Getters e Setters Atualizados ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venda getVenda() { return venda; }
    public void setVenda(Venda venda) { this.venda = venda; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(Double precoUnitario) { this.precoUnitario = precoUnitario; }

    public Double getDesconto() { return desconto; }
    public void setDesconto(Double desconto) { this.desconto = desconto; }

    // Getter e Setter do Custo (Essencial para a classe Venda compilar)
    public Double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(Double custoUnitario) { this.custoUnitario = custoUnitario; }
}