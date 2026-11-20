package com.rinconsur.medicion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// El caso completo en un servicio: las seis vistas cruzan conteos, sesiones y eventos, así que
// separarlas obligaría a reconstruir ese cruce por HTTP o a duplicar filas.
@SpringBootApplication
@EnableFeignClients
@OpenAPIDefinition(info = @Info(
    title = "Rincón Sur - API de Medición",
    version = "1.0",
    description = "Sesiones de observación, registros, eventos, carga de la tienda e indicadores"
))
public class MedicionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicionApplication.class, args);
    }
}
