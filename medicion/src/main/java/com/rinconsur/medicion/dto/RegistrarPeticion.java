package com.rinconsur.medicion.dto;

import jakarta.validation.constraints.NotNull;

// El toque que llega del pasillo: quién, de qué rango, con quién venía y si llevaba bolsas de
// otra tienda. La clasificación (visita o tránsito) es del observador, no la adivina el servicio.
public record RegistrarPeticion(
    @NotNull(message = "Indica la sesión.") Integer idSesion,
    @NotNull(message = "Indica la clasificación.") Integer idClasificacion,
    @NotNull(message = "Indica el rango etario.") Integer idRango,
    @NotNull(message = "Indica el tipo de compañía.") Integer idTipoCompania,
    Boolean comprasOtraTienda,
    Integer idAccesoDestino
) { }
