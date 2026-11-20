package com.rinconsur.medicion.dto;

import java.time.LocalTime;

// La hora la manda el dispositivo; si no viene, se usa la del servidor.
public record HoraPeticion(LocalTime hora) {

    public LocalTime oAhora() {
        return hora == null ? LocalTime.now().withSecond(0).withNano(0) : hora;
    }
}
