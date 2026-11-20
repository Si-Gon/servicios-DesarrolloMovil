package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.TransaccionBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransaccionRepositorio extends JpaRepository<TransaccionBloque, Integer> {

    List<TransaccionBloque> findByFechaOrderByIdBloqueAsc(LocalDate fecha);

    @Query("SELECT COALESCE(SUM(t.cantidad), 0) FROM TransaccionBloque t WHERE t.fecha = :fecha")
    long totalDelDia(@Param("fecha") LocalDate fecha);
}
