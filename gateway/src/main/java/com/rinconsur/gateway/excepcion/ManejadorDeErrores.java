package com.rinconsur.gateway.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

// Cuando un servicio está abajo, Netty lanza un IOException y sin este manejador el front
// recibe un 500 opaco. Con esto recibe 503 y un motivo que se puede mostrar.
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler({IOException.class, TimeoutException.class})
    public ResponseEntity<Map<String, String>> servicioAbajo(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("motivo", "El servicio no está disponible en este momento."));
    }
}
