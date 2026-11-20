package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.SesionVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
@Tag(name = "Sesiones", description = "Programación, cierre y validación de la observación")
public class SesionesController {

    private static final int FINALIZADA = 3;

    private final SesionRepositorio sesiones;
    private final RegistroRepositorio registros;

    public SesionesController(SesionRepositorio sesiones, RegistroRepositorio registros) {
        this.sesiones = sesiones;
        this.registros = registros;
    }

    @GetMapping
    @Operation(summary = "Las sesiones de una fecha (por defecto, hoy)")
    public List<SesionVista> deFecha(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        return sesiones.findByFechaOrderByHoraInicioProgAsc(dia).stream()
            .map(s -> SesionVista.de(s, registros.countByIdSesionAndAnuladoFalse(s.getId())))
            .toList();
    }

    @GetMapping("/por-validar")
    @Operation(summary = "Sesiones cerradas por el observador que esperan revisión del supervisor")
    public List<SesionVista> porValidar() {
        return sesiones.findByEstadoIdOrderByFechaAscHoraInicioProgAsc(FINALIZADA).stream()
            .map(s -> SesionVista.de(s, registros.countByIdSesionAndAnuladoFalse(s.getId())))
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Una sesión con su estado y el nombre del observador")
    public SesionVista porId(@PathVariable Integer id) {
        SesionObservacion s = sesiones.findById(id)
            .orElseThrow(() -> ErrorDeNegocio.noEncontrado("No existe una sesión con ese identificador."));
        return SesionVista.de(s, registros.countByIdSesionAndAnuladoFalse(id));
    }
}
