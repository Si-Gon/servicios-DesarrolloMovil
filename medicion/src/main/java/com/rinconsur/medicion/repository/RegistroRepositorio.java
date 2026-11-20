package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.RegistroObservacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroRepositorio extends JpaRepository<RegistroObservacion, Integer> {

    long countByIdSesionAndAnuladoFalse(Integer idSesion);

    List<RegistroObservacion> findTop10ByIdSesionAndAnuladoFalseOrderByFechaHoraDesc(Integer idSesion);

    long countByIdSesion(Integer idSesion);
}
