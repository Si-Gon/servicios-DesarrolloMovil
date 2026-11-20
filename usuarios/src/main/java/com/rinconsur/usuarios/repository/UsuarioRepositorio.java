package com.rinconsur.usuarios.repository;

import com.rinconsur.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Integer> {

    // El rol se trae en la misma consulta: sin esto, listar personas dispara una consulta por fila.
    @EntityGraph(attributePaths = "rol")
    List<Usuario> findAllByOrderByRolIdAscNombresAsc();

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByRut(String rut);

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findById(Integer id);

    boolean existsByRut(String rut);

    boolean existsByCorreo(String correo);
}
