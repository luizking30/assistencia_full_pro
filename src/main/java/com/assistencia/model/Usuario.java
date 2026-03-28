package com.assistencia.model;

import jakarta.persistence.*;

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
    private String password; // Alterado para 'password' (padrão Spring Security)

    private String role; // "ROLE_ADMIN" ou "ROLE_FUNCIONARIO"

    @Column(nullable = false)
    private boolean aprovado = false; // Novo: Controla a fila de espera (Inicia como falso)

    // --- CONSTRUTORES ---
    public Usuario() {}

    public Usuario(String nome, String username, String password, String role, boolean aprovado) {
        this.nome = nome;
        this.username = username;
        this.password = password;
        this.role = role;
        this.aprovado = aprovado;
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
}