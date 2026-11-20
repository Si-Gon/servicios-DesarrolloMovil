package com.rinconsur.medicion.dto;

import com.rinconsur.medicion.model.RegistroObservacion;

import java.time.LocalDateTime;

public record RegistroVista(
    Integer id,
    Integer idSesion,
    LocalDateTime fechaHora,
    Integer idClasificacion,
    Integer idRango,
    Integer idTipoCompania,
    Boolean comprasOtraTienda,
    Integer idAccesoDestino,
    Boolean anulado
) {
    public static RegistroVista de(RegistroObservacion r) {
        return new RegistroVista(r.getId(), r.getIdSesion(), r.getFechaHora(), r.getIdClasificacion(),
            r.getIdRango(), r.getIdTipoCompania(), r.getComprasOtraTienda(), r.getIdAccesoDestino(),
            r.getAnulado());
    }
}
