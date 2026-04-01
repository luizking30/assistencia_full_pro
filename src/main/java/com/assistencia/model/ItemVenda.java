package com.assistencia.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "item_venda")
@Data // Gera Getters, Setters, Equals, HashCode e ToString automaticamente
@NoArgsConstructor // Gera construtor vazio necessário para o JPA
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    @ToString.Exclude // Evita erro de recursão ao dar print na Venda
    @EqualsAndHashCode.Exclude
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private Integer quantidade;

    // Preço FINAL que o cliente pagou por UNIDADE (já calculado com o desconto)
    private Double precoUnitario = 0.0;

    // PORCENTAGEM do desconto aplicada neste item (ex: 5.0 para 5%)
    private Double desconto = 0.0;

    // Valor de entrada do produto no estoque (Preço de Custo no ato da venda)
    // 🚀 Essencial para calcular o lucro líquido real no Dashboard
    @Column(name = "custo_unitario")
    private Double custoUnitario = 0.0;

    // Construtor personalizado para facilitar a criação via Controller, se necessário
    public ItemVenda(Venda venda, Produto produto, Integer quantidade, Double precoUnitario, Double custoUnitario, Double desconto) {
        this.venda = venda;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.custoUnitario = custoUnitario;
        this.desconto = desconto;
    }
}