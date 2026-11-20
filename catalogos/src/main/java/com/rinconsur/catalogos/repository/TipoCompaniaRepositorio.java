package com.rinconsur.catalogos.repository;

import com.rinconsur.catalogos.model.TipoCompania;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoCompaniaRepositorio extends JpaRepository<TipoCompania, Integer> {

    List<TipoCompania> findAllByOrderByIdAsc();
}
