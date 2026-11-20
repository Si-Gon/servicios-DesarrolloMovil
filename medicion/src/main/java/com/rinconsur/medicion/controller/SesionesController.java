package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.HoraPeticion;
import com.rinconsur.medicion.dto.ProgramarSesionPeticion;
import com.rinconsur.medicion.dto.RechazoPeticion;
import com.rinconsur.medicion.dto.SesionVista;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import com.rinconsur.medicion.sesion.SesionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
@Tag(name = "Sesiones", description = "Programación, cierre, validación y rechazo de la observación")
public class SesionesController {

    private static final int FINALIZADA = 3;

    private final SesionRepositorio sesiones;
    private final RegistroRepositorio registros;
    private final SesionServicio servicio;

    public SesionesController(SesionRepositorio sesiones, RegistroRepositorio registros,
                              SesionServicio servicio) {
        this.sesiones = sesiones;
        this.registros = registros;
        this.servicio = servicio;
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
    @Operation(summary = "Una sesión con su estado y la cantidad de registros válidos")
    public SesionVista porId(@PathVariable Integer id) {
        var s = servicio.buscar(id);
        return SesionVista.de(s, registros.countByIdSesionAndAnuladoFalse(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Programa una sesión; el bloque se deduce de la hora de inicio")
    public SesionVista programar(@Valid @RequestBody ProgramarSesionPeticion peticion,
                                 @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
        return servicio.programar(peticion, idUsuario);
    }

    @PatchMapping("/{id}/iniciar")
    @Operation(summary = "Marca el inicio real de la sesión")
    public SesionVista iniciar(@PathVariable Integer id,
                               @RequestBody(required = false) HoraPeticion hora,
                               @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
        var cuando = hora == null ? java.time.LocalTime.now().withSecond(0).withNano(0) : hora.oAhora();
        return servicio.iniciar(id, idUsuario, cuando);
    }

    @PatchMapping("/{id}/cerrar")
    @Operation(summary = "Cierra la sesión; exige el mínimo de 30 registros del caso")
    public SesionVista cerrar(@PathVariable Integer id,
                              @RequestBody(required = false) HoraPeticion hora,
                              @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
        var cuando = hora == null ? java.time.LocalTime.now().withSecond(0).withNano(0) : hora.oAhora();
        return servicio.cerrar(id, idUsuario, cuando);
    }

    @PatchMapping("/{id}/validar")
    @Operation(summary = "Valida una sesión cerrada; nadie valida la que observó")
    public SesionVista validar(@PathVariable Integer id,
                               @RequestHeader(value = "X-Usuario-Id", required = false) Integer idRevisor) {
        return servicio.validar(id, idRevisor);
    }

    @PatchMapping("/{id}/rechazar")
    @Operation(summary = "Rechaza una sesión cerrada, con el motivo escrito")
    public SesionVista rechazar(@PathVariable Integer id,
                                @Valid @RequestBody RechazoPeticion peticion,
                                @RequestHeader(value = "X-Usuario-Id", required = false) Integer idRevisor) {
        String motivo = peticion.motivo().trim()
            + (peticion.detalle() == null || peticion.detalle().isBlank() ? "" : ": " + peticion.detalle().trim());
        return servicio.rechazar(id, idRevisor, motivo);
    }
}
