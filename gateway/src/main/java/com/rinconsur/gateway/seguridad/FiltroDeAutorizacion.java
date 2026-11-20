package com.rinconsur.gateway.seguridad;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Corre antes de las rutas (-100): si la petición no está autorizada no llega al servicio.
// Lo que sí pasa lleva el identificador y el perfil en cabeceras, para que el servicio dueño
// no tenga que preguntarle nada a usuarios.
@Component
public class FiltroDeAutorizacion implements GlobalFilter, Ordered {

    private final ValidadorDeToken validador;
    private final PoliticaDeAcceso politica;

    public FiltroDeAutorizacion(ValidadorDeToken validador, PoliticaDeAcceso politica) {
        this.validador = validador;
        this.politica = politica;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest peticion = exchange.getRequest();
        String metodo = peticion.getMethod().name();
        String ruta = peticion.getURI().getPath();

        if (politica.esPublica(metodo, ruta)) {
            return chain.filter(exchange);
        }

        String cabecera = peticion.getHeaders().getFirst("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return rechazar(exchange, HttpStatus.UNAUTHORIZED,
                "Falta el token o no es válido.");
        }

        Optional<Claims> claims = validador.validar(cabecera.substring("Bearer ".length()).trim());
        if (claims.isEmpty()) {
            return rechazar(exchange, HttpStatus.UNAUTHORIZED,
                "El token venció o no es válido. Vuelve a iniciar sesión.");
        }

        Claims datos = claims.get();
        String rol = String.valueOf(datos.get("rol"));
        if (politica.esSoloAdministrador(metodo, ruta) && !politica.esAdministrador(rol)) {
            return rechazar(exchange, HttpStatus.FORBIDDEN,
                "Esta operación requiere el perfil de administración.");
        }

        ServerHttpRequest conIdentidad = peticion.mutate()
            .header("X-Usuario-Id", String.valueOf(datos.get("id")))
            .header("X-Usuario-Rol", rol)
            .header("X-Usuario-Rut", String.valueOf(datos.getSubject()))
            .build();
        return chain.filter(exchange.mutate().request(conIdentidad).build());
    }

    // El cuerpo del error se escribe acá porque el filtro corre antes de que exista un
    // controlador que pueda lanzar una excepción.
    private Mono<Void> rechazar(ServerWebExchange exchange, HttpStatus estado, String motivo) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String cuerpo = "{\"motivo\":\"" + motivo + "\"}";
        DataBuffer bufer = respuesta.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return respuesta.writeWith(Mono.just(bufer));
    }
}
