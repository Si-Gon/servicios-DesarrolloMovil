package com.rinconsur.medicion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularPeticion(
    @NotBlank(message = "Escribe el motivo de la anulación.") String motivo,
    @Size(min = 5, message = "El motivo tiene que explicar qué pasó (5 caracteres o más).") String detalle
) { }
