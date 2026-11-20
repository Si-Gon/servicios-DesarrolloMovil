package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.AnularPeticion;
import com.rinconsur.medicion.dto.RegistrarPeticion;
import com.rinconsur.medicion.dto.RegistroVista;
import com.rinconsur.medicion.registro.RegistroServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registros")
@Tag(name = "Registros", description = "Los toques que anota el observador en el pasillo")
public class RegistrosController {

    private final RegistroServicio servicio;

    public RegistrosController(RegistroServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anota una persona observada en una sesión en curso")
    public RegistroVista registrar(@Valid @RequestBody RegistrarPeticion peticion,
                                   @RequestHeader(value = "X-Usuario-Id", required = false) Integer idObservador) {
        return servicio.registrar(peticion, idObservador);
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Anula un registro con su motivo; no lo borra")
    public RegistroVista anular(@PathVariable Integer id,
                                @Valid @RequestBody AnularPeticion peticion,
                                @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
        String motivo = peticion.motivo().trim()
            + (peticion.detalle() == null || peticion.detalle().isBlank() ? "" : ": " + peticion.detalle().trim());
        return servicio.anular(id, idUsuario, motivo);
    }

    @GetMapping("/sesion/{idSesion}/ultimos")
    @Operation(summary = "Los últimos diez registros válidos de una sesión")
    public List<RegistroVista> ultimos(@PathVariable Integer idSesion) {
        return servicio.ultimos(idSesion);
    }
}
