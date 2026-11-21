package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.ReporteDto;
import com.rinconsur.medicion.repository.ReporteRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/indicadores")
@Tag(name = "Reporte", description = "Meta del mes, comparación por mes y perfil del tránsito")
public class ReporteController {

    private final ReporteRepositorio reporte;

    public ReporteController(ReporteRepositorio reporte) {
        this.reporte = reporte;
    }

    @GetMapping("/reporte/{fecha}")
    @Operation(summary = "Reporte de jefatura del mes de la fecha indicada; si ese mes no tiene cifras, del último con datos")
    public ReporteDto reporte(@PathVariable LocalDate fecha) {
        String periodo = String.format("%04d-%02d", fecha.getYear(), fecha.getMonthValue());

        // Un mes sin cifras cargadas no puede mostrar meta ni perfil. En vez de una pantalla vacía
        // se informa el último mes que sí tiene datos, y el reporte dice cuál es.
        if (reporte.avanceDelMes(periodo).stream().allMatch(f -> f[0] == null || ((Number) f[0]).intValue() == 0)) {
            String ultimo = reporte.ultimoMesConDatos();
            if (ultimo != null) periodo = ultimo;
        }
        int anio = Integer.parseInt(periodo.substring(0, 4));
        int mes = Integer.parseInt(periodo.substring(5, 7));

        BigDecimal objetivo = reporte.metaDelMes(anio, mes).stream()
            .findFirst()
            .map(v -> v == null ? null : new BigDecimal(v.toString()))
            .orElse(null);

        int dias = 0;
        BigDecimal actual = null;
        List<Object[]> avance = reporte.avanceDelMes(periodo);
        if (!avance.isEmpty()) {
            Object[] f = avance.get(0);
            dias = f[0] == null ? 0 : ((Number) f[0]).intValue();
            actual = f[1] == null ? null : new BigDecimal(f[1].toString());
        }

        return new ReporteDto(
            new ReporteDto.Meta(anio, mes, objetivo, actual, dias),
            reporte.meses().stream().map(f -> new ReporteDto.Mes(
                String.valueOf(f[0]), largo(f[1]), largo(f[2]),
                f[3] == null ? null : new BigDecimal(f[3].toString()), (int) largo(f[4])
            )).toList(),
            reporte.horas(periodo).stream().map(f -> new ReporteDto.Hora(
                (int) largo(f[0]), largo(f[1]), largo(f[2]), largo(f[3]), largo(f[4])
            )).toList(),
            grupos(reporte.porRango(periodo)),
            grupos(reporte.porCompania(periodo)),
            grupos(reporte.compras(periodo))
        );
    }

    // Una vista devuelve nulos cuando un bloque no tiene información: se traducen a cero en vez
    // de dejar caer la petición entera.
    private long largo(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private List<ReporteDto.Grupo> grupos(List<Object[]> filas) {
        return filas.stream().map(f -> new ReporteDto.Grupo(
            String.valueOf(f[0]),
            f[1] == null ? 0 : ((Number) f[1]).longValue(),
            f[2] == null ? null : new BigDecimal(f[2].toString())
        )).toList();
    }
}
