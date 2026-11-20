package com.rinconsur.usuarios.controller;

import com.rinconsur.usuarios.dto.LoginPeticion;
import com.rinconsur.usuarios.dto.LoginRespuesta;
import com.rinconsur.usuarios.dto.UsuarioVista;
import com.rinconsur.usuarios.model.PreguntaSeguridad;
import com.rinconsur.usuarios.model.Usuario;
import com.rinconsur.usuarios.repository.PreguntaSeguridadRepositorio;
import com.rinconsur.usuarios.seguridad.CuentasServicio;
import com.rinconsur.usuarios.seguridad.JwtServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y preguntas de seguridad")
public class AuthController {

    private final CuentasServicio cuentas;
    private final JwtServicio jwt;
    private final PreguntaSeguridadRepositorio preguntas;

    public AuthController(CuentasServicio cuentas, JwtServicio jwt, PreguntaSeguridadRepositorio preguntas) {
        this.cuentas = cuentas;
        this.jwt = jwt;
        this.preguntas = preguntas;
    }

    @PostMapping("/login")
    @Operation(summary = "Valida RUT y clave, y devuelve el token con el perfil dentro")
    public LoginRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
        Usuario u = cuentas.autenticar(peticion.rut(), peticion.clave());
        // Si la cuenta viene de una creación del administrador, el token sale igual pero con
        // la marca: la app tiene que pedir clave nueva y pregunta antes de mostrar nada.
        return new LoginRespuesta(jwt.emitir(u), UsuarioVista.de(u));
    }

    @GetMapping("/preguntas")
    @Operation(summary = "Preguntas de seguridad activas para el primer acceso")
    public List<PreguntaSeguridad> preguntas() {
        return preguntas.findByActivaTrueOrderByIdAsc();
    }
}
