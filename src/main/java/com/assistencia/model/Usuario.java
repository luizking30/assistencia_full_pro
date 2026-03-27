package com.assistencia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;     // Adicionado: Nome real do funcionário (ex: "João Silva")

    @Column(unique = true)
    private String username; // Alterado de 'usuario' para 'username' (Padrão Spring Security)

    private String senha;
    private String role;     // "ROLE_ADMIN" ou "ROLE_FUNCIONARIO"

    // --- CONSTRUTOR ---
    public Usuario() {}

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; } // Resolve o erro: method getNome()
    public void setNome(String nome) { this.nome = nome; }

    public String getUsername() { return username; } // Resolve o erro: method getUsername()
    public void setUsername(String username) { this.username = username; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}