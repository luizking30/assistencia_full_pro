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

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal = 0.0;

    // Se o erro for "Field 'vendedor' doesn't have a default value",
    // este @Column garante a integridade.
    @Column(name = "vendedor", nullable = false)
    private String vendedor;

    // CascadeType.ALL é vital para que os itens sejam salvos junto com a venda
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemVenda> itens = new ArrayList<>();

    // Construtor padrão obrigatório para o JPA
    public Venda() {
        this.dataHora = LocalDateTime.now();
        this.valorTotal = 0.0;
    }

    /**
     * MÉTODO ESSENCIAL: O Spring/Thymeleaf preenche a lista de itens,
     * mas não seta a referência da "venda" dentro de cada item automaticamente.
     * Precisamos fazer isso antes de salvar para evitar o Erro 500.
     */
    @PrePersist
    @PreUpdate
    public void validarDadosAntesDeSalvar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        if (this.vendedor == null || this.vendedor.trim().isEmpty()) {
            this.vendedor = "Sistema Shark"; // Fallback de segurança
        }

        if (itens != null) {
            double totalCalculado = 0.0;
            for (ItemVenda item : itens) {
                // Vincula o item à venda pai (Obrigatório para evitar erro de Transient)
                item.setVenda(this);

                // Recalcula o total por segurança
                double preco = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
                int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;
                totalCalculado += (preco * qtd);
            }
            this.valorTotal = totalCalculado;
        }
    }
}