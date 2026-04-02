package com.assistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent; // Importação para validação de data
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

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
    @FutureOrPresent(message = "A Shark não aceita datas no passado!") // Bloqueia datas retroativas
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate dataVencimento;

    private boolean recorrente; // Se a conta se repete todo mês

    private boolean pago; // Status: Pendente (false) ou Pago (true)

    // Construtor Padrão (Obrigatório para o JPA)
    public Conta() {
    }

    // Construtor com Campos
    public Conta(String descricao, Double valor, LocalDate dataVencimento, boolean recorrente, boolean pago) {
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.recorrente = recorrente;
        this.pago = pago;
    }

    // --- GETTERS E SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public boolean isRecorrente() {
        return recorrente;
    }

    public void setRecorrente(boolean recorrente) {
        this.recorrente = recorrente;
    }

    public boolean isPago() {
        return pago;
    }

    public void setPago(boolean pago) {
        this.pago = pago;
    }
}