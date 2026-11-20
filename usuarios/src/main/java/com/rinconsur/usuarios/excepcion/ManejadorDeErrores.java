package com.rinconsur.usuarios.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

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

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> deBase(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("motivo", "Ese dato ya está registrado en otra cuenta."));
    }
}
