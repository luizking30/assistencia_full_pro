package com.assistencia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clienteNome;
    private String clienteCpf;
    private String clienteWhatsapp;

    private String produto;
    private String defeito;

    private String status;

    private LocalDate data;

    // getters e setters
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

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}