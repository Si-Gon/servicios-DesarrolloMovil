package com.rinconsur.catalogos.repository;

import com.rinconsur.catalogos.model.Clasificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClasificacionRepositorio extends JpaRepository<Clasificacion, Integer> {

    List<Clasificacion> findAllByOrderByIdAsc();
}
