package com.rinconsur.catalogos.excepcion;

import org.springframework.http.HttpStatus;

// Un solo tipo de error con fábricas por código: el controlador lanza una línea y el
// manejador la traduce a {motivo} con el estado que corresponde.
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

    public HttpStatus getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }
}
