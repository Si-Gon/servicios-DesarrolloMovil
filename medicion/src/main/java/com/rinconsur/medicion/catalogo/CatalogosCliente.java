package com.rinconsur.medicion.catalogo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// La única llamada entre servicios del proyecto, y es de lectura: medición pregunta por los
// accesos y los bloques para validar que existan antes de escribir. La dirección la resuelve
// Eureka por el nombre "catalogos"; no hay URL escrita a mano.
@FeignClient(name = "catalogos")
public interface CatalogosCliente {

    @GetMapping("/api/catalogos/accesos")
    List<AccesoDto> accesos();

    @GetMapping("/api/catalogos/bloques")
    List<BloqueDto> bloques();

    record AccesoDto(Integer id, String codigo, String nombre, String ubicacion, String observacion) { }

    record BloqueDto(Integer id, java.time.LocalTime horaInicio, java.time.LocalTime horaFin) { }
}
