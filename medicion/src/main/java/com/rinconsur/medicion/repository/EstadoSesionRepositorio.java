package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.EstadoSesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoSesionRepositorio extends JpaRepository<EstadoSesion, Integer> {

    Optional<EstadoSesion> findByNombre(String nombre);
}
