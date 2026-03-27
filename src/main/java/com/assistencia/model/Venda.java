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

    @Column(name = "custo_total_estoque")
    private Double custoTotalEstoque = 0.0;

    @Column(name = "vendedor", nullable = false)
    private String vendedor;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda() {
        this.dataHora = LocalDateTime.now();
    }

    /**
     * Este método roda AUTOMATICAMENTE antes de salvar no banco.
     * Ele garante que o lucro e o custo da Shark Eletrônicos sejam calculados corretamente.
     */
    @PrePersist
    @PreUpdate
    public void validarDadosAntesDeSalvar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }

        // Garante que o vendedor não fique nulo para o relatório
        if (this.vendedor == null || this.vendedor.trim().isEmpty()) {
            this.vendedor = "Sistema Shark";
        }

        double totalVendaCalculado = 0.0;
        double totalCustoCalculado = 0.0;

        if (itens != null && !itens.isEmpty()) {
            for (ItemVenda item : itens) {
                // Essencial: Vincula o item à venda pai para o JPA salvar os itens
                item.setVenda(this);

                int qtd = (item.getQuantidade() != null) ? item.getQuantidade() : 0;

                // Preço de Venda (Entrada no Caixa)
                double precoVenda = (item.getPrecoUnitario() != null) ? item.getPrecoUnitario() : 0.0;
                totalVendaCalculado += (precoVenda * qtd);

                // Preço de Custo (Saída do Estoque/Bolso)
                // Certifique-se que o objeto ItemVenda tenha o campo 'custoUnitario'
                double precoCusto = (item.getCustoUnitario() != null) ? item.getCustoUnitario() : 0.0;
                totalCustoCalculado += (precoCusto * qtd);
            }
        }

        // Atualiza os campos que o Relatório Financeiro utiliza
        this.valorTotal = totalVendaCalculado;
        this.custoTotalEstoque = totalCustoCalculado;
    }
}