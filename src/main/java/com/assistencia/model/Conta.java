package com.assistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contas_apagar")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A descrição é obrigatória")
    @Column(nullable = false)
    private String descricao;

    @NotNull(message = "O valor não pode ser nulo")
    @Positive(message = "O valor deve ser maior que zero")
    @Column(nullable = false)
    private Double valor;

    @NotNull(message = "A data de vencimento é obrigatória")
    //@FutureOrPresent(message = "A Shark não aceita datas no passado!")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate dataVencimento;

    private boolean recorrente;

    // 🚀 AJUSTADO PARA BATER COM O SEU HTML (contas.html)
    private boolean paga;

    // --- NOVOS CAMPOS PARA AUDITORIA (HISTÓRICO) ---

    private String usuarioPagador;

    private LocalDateTime dataPagamento;

    // Construtor Padrão
    public Conta() {
    }

    // Construtor com Campos
    public Conta(String descricao, Double valor, LocalDate dataVencimento, boolean recorrente, boolean paga) {
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.recorrente = recorrente;
        this.paga = paga;
    }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }

    // GETTERS E SETTERS AJUSTADOS PARA "PAGA"
    public boolean isPaga() { return paga; }
    public void setPaga(boolean paga) { this.paga = paga; }

    // --- GETTERS E SETTERS DOS NOVOS CAMPOS ---

    public String getUsuarioPagador() { return usuarioPagador; }
    public void setUsuarioPagador(String usuarioPagador) { this.usuarioPagador = usuarioPagador; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }
}