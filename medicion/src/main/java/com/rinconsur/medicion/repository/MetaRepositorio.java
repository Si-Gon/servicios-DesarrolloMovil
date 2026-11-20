package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.MetaConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaRepositorio extends JpaRepository<MetaConversion, Integer> {

    Optional<MetaConversion> findByAnioAndMes(Short anio, Byte mes);
}
