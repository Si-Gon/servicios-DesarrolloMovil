package com.rinconsur.medicion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoPeticion(
    @NotBlank(message = "Escribe el motivo del rechazo.") String motivo,
    @Size(min = 10, message = "El motivo tiene que explicar la razón (10 caracteres o más).") String detalle
) { }
