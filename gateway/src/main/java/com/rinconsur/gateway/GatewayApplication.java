package com.rinconsur.gateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Puerta única de la aplicación: todo lo que la app móvil pide entra por acá (8090).
// El documento de Swagger de este módulo apunta a los tres servicios de negocio, para que
// quien evalúa abra una sola página y vea las tres APIs.
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
    title = "Rincón Sur - Puerta de entrada",
    version = "1.0",
    description = "Valida el token y reparte /api/** entre catálogos, usuarios y medición. "
        + "Las APIs de cada servicio están en este mismo Swagger, en el selector de arriba."
))
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
