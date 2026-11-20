package com.rinconsur.catalogos.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Todos los errores salen con la misma forma: {"motivo": "..."}. La app muestra ese texto
// tal cual, así que el mensaje se escribe pensando en quien lo va a leer.
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(ErrorDeNegocio.class)
    public ResponseEntity<Map<String, String>> deNegocio(ErrorDeNegocio e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("motivo", e.getMotivo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> deValidacion(MethodArgumentNotValidException e) {
        String motivo = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("Los datos enviados no son válidos.");
        return ResponseEntity.badRequest().body(Map.of("motivo", motivo));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> generico(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("motivo", "Ocurrió un problema en el servicio de catálogos."));
    }
}
