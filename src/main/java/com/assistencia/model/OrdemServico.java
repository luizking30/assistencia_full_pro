package com.assistencia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clienteNome;
    private String clienteCpf;
    private String clienteWhatsapp;
    private String produto;

    @Column(columnDefinition = "TEXT")
    private String defeito;

    @Column(length = 50)
    private String status;

    private LocalDateTime data; // Data de abertura
    private LocalDateTime dataEntrega; // Data de finalização/entrega

    private Double valorTotal; // Valor cobrado do cliente
    private Double custoPeca = 0.0; // Valor gasto com peças (Custo)

    // --- CONSTRUTOR ---
    public OrdemServico() {
        this.data = LocalDateTime.now();
        if (this.status == null) {
            this.status = "Em análise"; // Alinhado com o seu Dashboard
        }
    }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteCpf() { return clienteCpf; }
    public void setClienteCpf(String clienteCpf) { this.clienteCpf = clienteCpf; }

    public String getClienteWhatsapp() { return clienteWhatsapp; }
    public void setClienteWhatsapp(String clienteWhatsapp) { this.clienteWhatsapp = clienteWhatsapp; }

    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }

    public String getDefeito() { return defeito; }
    public void setDefeito(String defeito) { this.defeito = defeito; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public Double getCustoPeca() { return custoPeca; }
    public void setCustoPeca(Double custoPeca) { this.custoPeca = custoPeca; }

    // --- MÉTODOS DE COMPATIBILIDADE (ALIASE) ---
    // Mantidos para evitar erro em partes do código que usem .getValor()
    public Double getValor() { return valorTotal; }
    public void setValor(Double valor) { this.valorTotal = valor; }
}