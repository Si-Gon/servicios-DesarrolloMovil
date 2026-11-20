package com.rinconsur.catalogos.repository;

import com.rinconsur.catalogos.model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccesoRepositorio extends JpaRepository<Acceso, Integer> {

    List<Acceso> findAllByOrderByIdAsc();

    Optional<Acceso> findByCodigo(String codigo);
}
