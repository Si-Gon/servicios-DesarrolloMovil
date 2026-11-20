package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.SesionObservacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SesionRepositorio extends JpaRepository<SesionObservacion, Integer> {

    @EntityGraph(attributePaths = "estado")
    List<SesionObservacion> findByFechaOrderByHoraInicioProgAsc(LocalDate fecha);

    // Las que esperan revisión: cerradas por el observador y todavía sin validar.
    @EntityGraph(attributePaths = "estado")
    List<SesionObservacion> findByEstadoIdOrderByFechaAscHoraInicioProgAsc(Integer idEstado);

    @EntityGraph(attributePaths = "estado")
    List<SesionObservacion> findAllByOrderByFechaDescHoraInicioProgAsc();

    boolean existsByCodigo(String codigo);

    long countByFecha(LocalDate fecha);

    @EntityGraph(attributePaths = "estado")
    java.util.Optional<SesionObservacion> findById(Integer id);
}
