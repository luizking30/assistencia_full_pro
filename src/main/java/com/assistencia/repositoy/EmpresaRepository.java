package com.assistencia.repository;

import com.assistencia.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // Busca apenas empresas que estão com cadastro ativo
    List<Empresa> findByAtivoTrue();
}