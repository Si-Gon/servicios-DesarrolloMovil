package com.rinconsur.medicion.dto;

import com.rinconsur.medicion.model.SesionObservacion;

import java.time.LocalDate;
import java.time.LocalTime;

// Lo que la lista de sesiones necesita mostrar. El nombre del acceso no viene de acá: la app
// ya tiene el catálogo y lo resuelve con el id, así este servicio no depende de otro para pintar.
public record SesionVista(
    Integer id,
    String codigo,
    Integer idAcceso,
    Integer idBloque,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String estado,
    String observador,
    long registros
) {
    public static SesionVista de(SesionObservacion s, long registros) {
        return new SesionVista(
            s.getId(), s.getCodigo(), s.getIdAcceso(), s.getIdBloque(), s.getFecha(),
            s.getHoraInicioProg(), s.getHoraFinProg(), s.getEstado().getNombre(),
            s.getObservadorNombre(), registros
        );
    }
}
