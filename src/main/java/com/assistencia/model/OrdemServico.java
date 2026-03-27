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

    private LocalDateTime data;
    private LocalDateTime dataEntrega;

    private Double valorTotal;
    private Double custoPeca = 0.0;

    // --- ADICIONADO: Relacionamento com o Vendedor/Usuário ---
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public OrdemServico() {
        this.data = LocalDateTime.now();
        if (this.status == null) {
            this.status = "Em análise";
        }
    }

    // --- GETTERS E SETTERS ORIGINAIS MANTIDOS ---
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

    // --- NOVO GETTER E SETTER ---
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Double getValor() { return valorTotal; }
    public void setValor(Double valor) { this.valorTotal = valor; }
}