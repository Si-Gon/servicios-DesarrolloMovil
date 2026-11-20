package com.rinconsur.medicion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// El bloque no se pide: lo deduce el servicio a partir de la hora de inicio.
public record ProgramarSesionPeticion(
    @NotNull(message = "Indica el acceso de la sesión.") Integer idAcceso,
    @NotNull(message = "Indica la fecha de la sesión.") LocalDate fecha,
    @NotNull(message = "Indica la hora de inicio.") LocalTime horaInicio,
    @NotNull(message = "Indica la hora de término.") LocalTime horaFin,
    @NotNull(message = "Indica quién va a observar.") Integer idObservador,
    // La app manda el nombre de la persona que acaba de elegir; se guarda como foto del momento
    // para que la lista de sesiones no tenga que consultar el servicio de usuarios.
    String observadorNombre
) { }
