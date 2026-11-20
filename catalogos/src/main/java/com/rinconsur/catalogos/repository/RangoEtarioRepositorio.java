package com.rinconsur.catalogos.repository;

import com.rinconsur.catalogos.model.RangoEtario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RangoEtarioRepositorio extends JpaRepository<RangoEtario, Integer> {

    List<RangoEtario> findAllByOrderByOrdenAsc();
}
