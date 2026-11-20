package com.rinconsur.usuarios.controller;

import com.rinconsur.usuarios.dto.CrearUsuarioPeticion;
import com.rinconsur.usuarios.dto.UsuarioCreado;
import com.rinconsur.usuarios.dto.UsuarioVista;
import com.rinconsur.usuarios.model.Rol;
import com.rinconsur.usuarios.repository.RolRepositorio;
import com.rinconsur.usuarios.seguridad.CuentasServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Cuentas", description = "Personas de la tienda y sus perfiles")
public class UsuariosController {

    private final CuentasServicio cuentas;
    private final RolRepositorio roles;

    public UsuariosController(CuentasServicio cuentas, RolRepositorio roles) {
        this.cuentas = cuentas;
        this.roles = roles;
    }

    @GetMapping
    @Operation(summary = "Todas las cuentas, ordenadas por perfil y nombre")
    public List<UsuarioVista> listar() {
        return cuentas.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Una cuenta por su identificador")
    public UsuarioVista buscar(@PathVariable Integer id) {
        return cuentas.buscar(id);
    }

    @GetMapping("/roles")
    @Operation(summary = "Los cinco perfiles del caso, con su código")
    public List<Rol> roles() {
        return roles.findAllByOrderByIdAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea la cuenta y devuelve el código temporal para entregarlo en mano")
    public UsuarioCreado crear(@Valid @RequestBody CrearUsuarioPeticion peticion) {
        return cuentas.crear(peticion);
    }
}
