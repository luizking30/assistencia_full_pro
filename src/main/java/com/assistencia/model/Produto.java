package com.assistencia.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "produto")
@Data // Gera getPrecoVenda() e outros automaticamente
@NoArgsConstructor // Gera o construtor vazio
@AllArgsConstructor // Gera o construtor com todos os campos
public class Produto {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private Double precoCusto;
    private Double precoVenda;
    private Integer quantidade;
}