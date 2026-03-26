package com.assistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @Column(unique = true, nullable = false, length = 20)
    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;

    @Column(length = 20)
    private String whatsapp;

    // NOVO: Campo necessário para o Dashboard (Filtro por data)
    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    // --- Gatilho Automático ---
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }

    // --- Getters e Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}