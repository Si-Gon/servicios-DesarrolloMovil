package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.IndicadorDiaVista;
import com.rinconsur.medicion.repository.IndicadorRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indicadores")
@Tag(name = "Indicadores", description = "Los números del día, leídos de las vistas")
public class IndicadoresController {

    private final IndicadorRepositorio indicadores;

    public IndicadoresController(IndicadorRepositorio indicadores) {
        this.indicadores = indicadores;
    }

    @GetMapping("/{fecha}")
    @Operation(summary = "Conversión y tránsito de un día; null si la tienda no cargó sus cifras")
    public IndicadorDiaVista delDia(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<Object[]> filas = indicadores.filaDelDia(fecha);
        return filas.isEmpty() ? null : aVistaDelDia(filas.get(0), fecha);
    }

    @GetMapping
    @Operation(summary = "Agregado por tipo de día (Habil o Fin de semana), opcionalmente de una semana")
    public IndicadorDiaVista delTipo(
        @RequestParam String tipoDia,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        // Un rango abierto evita mandar parámetros nulos al SQL, que MySQL tipa de forma
        // inconsistente según el driver.
        LocalDate inicio = desde == null ? LocalDate.of(1900, 1, 1) : desde;
        LocalDate fin = hasta == null ? LocalDate.of(2999, 12, 31) : hasta;
        List<Object[]> filas = indicadores.filaDelTipo(tipoDia, inicio, fin);
        return filas.isEmpty() ? null : aVista(filas.get(0));
    }

    @GetMapping("/pendientes/{fecha}")
    @Operation(summary = "Bloques con ingresos cargados que todavía no tienen sesión validada")
    public List<Map<String, Object>> pendientes(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return indicadores.pendientesDeMedir(fecha).stream().map(f -> Map.of(
            "fecha", String.valueOf(f[1]),
            "idAcceso", f[0],
            "idBloque", f[2],
            "ultimaSesionValida", f[3] == null ? "" : String.valueOf(f[3]),
            "diasSinMedir", f[4] == null ? 0 : f[4],
            "semanasSinMedir", f[5] == null ? 0.0 : f[5]
        )).toList();
    }

    // El agregado trae desde/hasta y después los mismos campos del día.
    private IndicadorDiaVista aVista(Object[] f) {
        return new IndicadorDiaVista(
            String.valueOf(f[0]), String.valueOf(f[1]), String.valueOf(f[2]),
            ((Number) f[3]).longValue(), ((Number) f[4]).longValue(),
            (BigDecimal) f[5], String.valueOf(f[6]), (BigDecimal) f[7], (BigDecimal) f[8],
            (BigDecimal) f[9], ((Number) f[10]).intValue(), ((Number) f[11]).intValue(),
            ((Number) f[12]).intValue()
        );
    }

    // La fila del día no trae rango: se usa la fecha pedida para los dos extremos.
    private IndicadorDiaVista aVistaDelDia(Object[] f, LocalDate fecha) {
        return new IndicadorDiaVista(
            fecha.toString(), fecha.toString(),
            // La fila del día ya trae su fecha en la posición 0: se salta y se sigue desde tipo_dia.
            String.valueOf(f[1]),
            ((Number) f[2]).longValue(), ((Number) f[3]).longValue(),
            (BigDecimal) f[4], String.valueOf(f[5]), (BigDecimal) f[6], (BigDecimal) f[7],
            (BigDecimal) f[8], ((Number) f[9]).intValue(), ((Number) f[10]).intValue(),
            ((Number) f[11]).intValue()
        );
    }
}
