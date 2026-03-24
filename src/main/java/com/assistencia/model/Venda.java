package com.assistencia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String produto;
    private double valorVenda;
    private String funcionario;
    private LocalDateTime dataHora;

    public Long getId() { return id; }

    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }

    public double getValorVenda() { return valorVenda; }
    public void setValorVenda(double valorVenda) { this.valorVenda = valorVenda; }

    public String getFuncionario() { return funcionario; }
    public void setFuncionario(String funcionario) { this.funcionario = funcionario; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}