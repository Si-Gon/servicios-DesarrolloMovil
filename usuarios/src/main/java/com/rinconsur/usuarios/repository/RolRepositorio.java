package com.rinconsur.usuarios.repository;

import com.rinconsur.usuarios.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepositorio extends JpaRepository<Rol, Integer> {

    List<Rol> findAllByOrderByIdAsc();

    Optional<Rol> findByCodigo(String codigo);
}
