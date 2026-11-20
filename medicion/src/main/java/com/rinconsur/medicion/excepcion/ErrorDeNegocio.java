package com.rinconsur.medicion.excepcion;

import org.springframework.http.HttpStatus;

public class ErrorDeNegocio extends RuntimeException {

    private final HttpStatus estado;
    private final String motivo;

    private ErrorDeNegocio(HttpStatus estado, String motivo) {
        super(motivo);
        this.estado = estado;
        this.motivo = motivo;
    }

    public static ErrorDeNegocio noEncontrado(String motivo) {
        return new ErrorDeNegocio(HttpStatus.NOT_FOUND, motivo);
    }

    public static ErrorDeNegocio invalido(String motivo) {
        return new ErrorDeNegocio(HttpStatus.BAD_REQUEST, motivo);
    }

    public static ErrorDeNegocio conflicto(String motivo) {
        return new ErrorDeNegocio(HttpStatus.CONFLICT, motivo);
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }
}
