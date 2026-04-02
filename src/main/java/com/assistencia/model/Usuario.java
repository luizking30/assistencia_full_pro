package com.assistencia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // Ex: ROLE_ADMIN, ROLE_FUNCIONARIO

    @Column(nullable = false)
    private boolean aprovado = false;

    // Define a função na Shark: "VENDEDOR", "TECNICO" ou "HIBRIDO"
    @Column(length = 20)
    private String tipoFuncionario;

    // --- TAXAS DE PERCENTUAL ---
    @Column(nullable = false)
    private Double comissaoOs = 0.0;

    @Column(nullable = false)
    private Double comissaoVenda = 0.0;

    // --- CAMPOS DE CÁLCULO (TRANSIENTES: Não salvam no banco, servem para o HTML) ---

    @Transient
    private Double totalComissaoOsAcumulada = 0.0; // Agora representa o Líquido total a pagar

    @Transient
    private Double saldoVendaCalculado = 0.0;    // Agora representa o Líquido total a pagar

    @Transient
    private Double brutoVendaCalculado = 0.0;

    @Transient
    private Double brutoOsCalculado = 0.0;

    @Transient
    private Double totalPagoOs = 0.0;

    @Transient
    private Double totalPagoVenda = 0.0;

    // Novos campos para a lógica de "Zerar o Ciclo" e histórico
    @Transient
    private LocalDateTime dataUltimoPagamento;

    @Transient
    private Long diasSemPagamento;

    // --- MÉTODOS DE APOIO ---
    public Double getSaldoTotalReceber() {
        double os = (totalComissaoOsAcumulada != null) ? totalComissaoOsAcumulada : 0.0;
        double vendas = (saldoVendaCalculado != null) ? saldoVendaCalculado : 0.0;
        return os + vendas;
    }

    // --- CONSTRUTORES ---
    public Usuario() {}

    public Usuario(String nome, String username, String password, String role, boolean aprovado, String tipoFuncionario) {
        this.nome = nome;
        this.username = username;
        this.password = password;
        this.role = role;
        this.aprovado = aprovado;
        this.tipoFuncionario = tipoFuncionario;
    }

    // --- GETTERS E SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAprovado() { return aprovado; }
    public void setAprovado(boolean aprovado) { this.aprovado = aprovado; }

    public String getTipoFuncionario() { return tipoFuncionario; }
    public void setTipoFuncionario(String tipoFuncionario) { this.tipoFuncionario = tipoFuncionario; }

    public Double getComissaoOs() { return comissaoOs; }
    public void setComissaoOs(Double comissaoOs) { this.comissaoOs = comissaoOs; }

    public Double getComissaoVenda() { return comissaoVenda; }
    public void setComissaoVenda(Double comissaoVenda) { this.comissaoVenda = comissaoVenda; }

    public Double getTotalComissaoOsAcumulada() { return totalComissaoOsAcumulada; }
    public void setTotalComissaoOsAcumulada(Double totalComissaoOsAcumulada) { this.totalComissaoOsAcumulada = totalComissaoOsAcumulada; }

    public Double getSaldoVendaCalculado() { return saldoVendaCalculado; }
    public void setSaldoVendaCalculado(Double saldoVendaCalculado) { this.saldoVendaCalculado = saldoVendaCalculado; }

    public Double getBrutoVendaCalculado() { return brutoVendaCalculado; }
    public void setBrutoVendaCalculado(Double brutoVendaCalculado) { this.brutoVendaCalculado = brutoVendaCalculado; }

    public Double getBrutoOsCalculado() { return brutoOsCalculado; }
    public void setBrutoOsCalculado(Double brutoOsCalculado) { this.brutoOsCalculado = brutoOsCalculado; }

    public Double getTotalPagoOs() { return totalPagoOs; }
    public void setTotalPagoOs(Double totalPagoOs) { this.totalPagoOs = totalPagoOs; }

    public Double getTotalPagoVenda() { return totalPagoVenda; }
    public void setTotalPagoVenda(Double totalPagoVenda) { this.totalPagoVenda = totalPagoVenda; }

    public LocalDateTime getDataUltimoPagamento() { return dataUltimoPagamento; }
    public void setDataUltimoPagamento(LocalDateTime dataUltimoPagamento) { this.dataUltimoPagamento = dataUltimoPagamento; }

    public Long getDiasSemPagamento() { return diasSemPagamento; }
    public void setDiasSemPagamento(Long diasSemPagamento) { this.diasSemPagamento = diasSemPagamento; }
}