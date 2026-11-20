package com.rinconsur.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginPeticion(
    @NotBlank(message = "Escribe tu RUT.") String rut,
    @NotBlank(message = "Escribe tu clave.") String clave
) { }
