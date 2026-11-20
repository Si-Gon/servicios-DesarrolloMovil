package com.rinconsur.catalogos.excepcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

// Todos los errores del servicio salen con la misma forma: {"motivo": "..."} con tildes, escrito
// para quien lo va a leer en la app. Es el único consejo de excepciones del módulo: si hubiera dos,
// el formato dependería del orden en que Spring los elija.
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger registro = LoggerFactory.getLogger(ManejadorDeErrores.class);

    // Lo que el negocio decide: el estado y el texto ya vienen pensados desde el servicio.
    @ExceptionHandler(ErrorDeNegocio.class)
    public ResponseEntity<Map<String, String>> deNegocio(ErrorDeNegocio e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("motivo", e.getMotivo()));
    }

    // @Valid: se devuelve el primer mensaje del DTO, que está escrito en castellano.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> deValidacion(MethodArgumentNotValidException e) {
        String motivo = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("Los datos enviados no son válidos.");
        return ResponseEntity.badRequest().body(Map.of("motivo", motivo));
    }

    // El cuerpo no es JSON válido, o le falta un campo obligatorio del tipo.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> deCuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("motivo", "El cuerpo de la petición no es válido."));
    }

    // Un parámetro con el tipo equivocado (una fecha que no es fecha) o que falta.
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, String>> deParametro(Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("motivo", "Alguno de los datos enviados no es válido."));
    }

    // Unicidad o una regla de la tabla: se responde 409 y no se deja reventar como 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> deBase(DataIntegrityViolationException e) {
        registro.warn("Choque con una restricción de la base: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("motivo", "Ya existe un registro con ese código en los datos maestros."));
    }

    // Una ruta que no existe en este servicio.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> deRuta(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("motivo", "La ruta solicitada no existe en este servicio."));
    }

    // Lo inesperado: se anota completo en el log del servicio y al cliente le va un texto corto,
    // sin rastros de la base ni del stack.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> generico(Exception e) {
        registro.error("Error no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("motivo", "Ocurrió un problema en el servicio de catálogos."));
    }
}
