package com.rinconsur.catalogos.repository;

import com.rinconsur.catalogos.model.BloqueHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloqueHorarioRepositorio extends JpaRepository<BloqueHorario, Integer> {

    List<BloqueHorario> findAllByOrderByIdAsc();
}
