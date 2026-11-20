package com.rinconsur.catalogos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Datos maestros del caso: accesos, bloques horarios, clasificaciones, rangos y compañías.
// Los lee la app para sus desplegables y los lee el servicio de medición para validar que un
// código exista antes de escribir. Es la razón por la que son un servicio y no una copia.
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
    title = "Rincón Sur - API de Catálogos",
    version = "1.0",
    description = "Accesos, bloques horarios, clasificaciones, rangos etarios y tipos de compañía"
))
public class CatalogosApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogosApplication.class, args);
    }
}
