package com.rinconsur.medicion.dto;

import java.math.BigDecimal;
import java.util.List;

// El reporte de jefatura tal como lo consumen las pantallas. Los nombres de los rangos y de las
// compañías no vienen de acá: viven en el servicio de catálogos, y la app ya los tiene. El
// servicio entrega números e identificadores, no etiquetas duplicadas.
public record ReporteDto(
    Meta meta,
    List<Mes> meses,
    List<Hora> horas,
    List<Grupo> porRango,
    List<Grupo> porCompania,
    List<Grupo> compras
) {
    public record Meta(int anio, int mes, BigDecimal objetivo, BigDecimal actual, int dias) { }

    public record Mes(String mes, long ingresos, long boletas, BigDecimal conversion, int dias) { }

    // El bloque va por su identificador: la hora de inicio está en catálogos.
    public record Hora(int idBloque, long ingresos, long transito, long medidos, long bloques) { }

    // etiqueta: el id del rango, de la compañía o "compro"/"no compro" en las compras.
    public record Grupo(String etiqueta, long cantidad, BigDecimal porcentaje) { }
}
