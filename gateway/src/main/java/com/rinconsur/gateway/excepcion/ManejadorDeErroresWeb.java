package com.rinconsur.gateway.excepcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

// El gateway es reactivo: los errores no pasan por un @RestControllerAdvice, sino por el
// manejador web. Este formatea el 404 de una ruta que no tiene dueño con el mismo cuerpo que el
// resto de la API. Va antes que el manejador por defecto de Spring.
@Component
@Order(-2)
public class ManejadorDeErroresWeb implements ErrorWebExceptionHandler {

    private static final Logger registro = LoggerFactory.getLogger(ManejadorDeErroresWeb.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatusCode estado = ex instanceof ResponseStatusException rse
            ? rse.getStatusCode()
            : HttpStatus.INTERNAL_SERVER_ERROR;

        String motivo;
        if (estado.value() == 404) {
            motivo = "La ruta solicitada no existe en la API.";
        } else if (estado.is5xxServerError()) {
            registro.error("Error no controlado en la puerta de entrada", ex);
            motivo = "Ocurrió un problema en la puerta de entrada.";
        } else {
            motivo = "La petición no se pudo procesar.";
        }

        exchange.getResponse().setStatusCode(estado);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer cuerpo = exchange.getResponse().bufferFactory()
            .wrap(("{\"motivo\":\"" + motivo + "\"}").getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(cuerpo));
    }
}
