package com.rinconsur.medicion.dto;

import java.math.BigDecimal;

// Una fila de la vista v_conversion_dia, tal como la calcula la base. El servicio no recalcula:
// si la app y el servicio usaran fórmulas distintas, dos pantallas mostrarían números distintos.
public record IndicadorDiaVista(
    String desde,
    String hasta,
    String tipoDia,
    long ingresos,
    long transacciones,
    BigDecimal conversionBruta,
    String dia,
    BigDecimal transito,
    BigDecimal flujoSinTransito,
    BigDecimal conversionSinTransito,
    int medidos,
    int estimados,
    int sinInformacion
) { }
