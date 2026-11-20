package com.rinconsur.usuarios.repository;

import com.rinconsur.usuarios.model.PreguntaSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaSeguridadRepositorio extends JpaRepository<PreguntaSeguridad, Integer> {

    List<PreguntaSeguridad> findByActivaTrueOrderByIdAsc();
}
