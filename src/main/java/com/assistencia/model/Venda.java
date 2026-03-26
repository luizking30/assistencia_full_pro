package com.assistencia.model;

import jakarta.persistence.*;
import lombok.Data;
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

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @Column(name = "valor_total")
    private Double valorTotal = 0.0;

    /**
     * AJUSTE AQUI:
     * O erro 1364 diz que o campo 'vendedor' não tem valor padrão.
     * Portanto, o 'name' dentro da anotação DEVE ser 'vendedor'.
     */
    @Column(name = "vendedor", nullable = false)
    private String vendedor;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        // Se o Controller (Authentication auth) não enviar o nome do vendedor,
        // este fallback garante que o banco não rejeite a gravação.
        if (this.vendedor == null || this.vendedor.trim().isEmpty()) {
            this.vendedor = "Shark Admin";
        }
    }

    public void adicionarItem(ItemVenda item) {
        if (item != null) {
            itens.add(item);
            item.setVenda(this);

            double preco = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
            int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;

            if (this.valorTotal == null) this.valorTotal = 0.0;
            this.valorTotal += (preco * qtd);
        }
    }

    public Venda() {}
}