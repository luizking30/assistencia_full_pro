package com.assistencia.model; // Verifique se esta linha é a primeira do arquivo

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

    private LocalDateTime dataEntrega; // Para o relatório de serviços entregues

    private Double valorTotal; // Alinhado com o seu Controller e Relatório

    // Construtor Padrão (Obrigatório para o JPA/Hibernate)
    public OrdemServico() {
        this.data = LocalDateTime.now();
        this.status = "ABERTA";
    }

    // --- Getters e Setters ---

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

    // Método de compatibilidade (Alias) para evitar erros se houver chamadas para 'getValor'
    public Double getValor() { return valorTotal; }
    public void setValor(Double valor) { this.valorTotal = valor; }
}