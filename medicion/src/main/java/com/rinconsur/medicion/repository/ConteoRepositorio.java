package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.ConteoIngreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ConteoRepositorio extends JpaRepository<ConteoIngreso, Integer> {

    List<ConteoIngreso> findByFechaOrderByIdBloqueAsc(LocalDate fecha);

    @Query("SELECT COALESCE(SUM(c.cantidad), 0) FROM ConteoIngreso c WHERE c.fecha = :fecha")
    long totalDelDia(@Param("fecha") LocalDate fecha);
}
