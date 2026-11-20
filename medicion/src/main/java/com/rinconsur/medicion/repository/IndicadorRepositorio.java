package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.dto.IndicadorDiaVista;
import com.rinconsur.medicion.model.SesionObservacion;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

// Las vistas son el contrato del cálculo: el servicio las lee y no reimplementa la fórmula.
public interface IndicadorRepositorio extends Repository<SesionObservacion, Integer> {

    @Query(value = """
        SELECT fecha, tipo_dia, ingresos, transacciones, conversion_bruta, dia, transito,
               flujo_sin_transito, conversion_sin_transito, medidos, estimados, sin_informacion
        FROM v_conversion_dia
        WHERE fecha = :fecha
        """, nativeQuery = true)
    List<Object[]> filaDelDia(@Param("fecha") LocalDate fecha);

    @Query(value = """
        SELECT MIN(fecha) AS desde, MAX(fecha) AS hasta, tipo_dia, SUM(ingresos), SUM(transacciones),
               ROUND(100.0 * SUM(transacciones) / NULLIF(SUM(ingresos), 0), 1) AS conversion_bruta,
               CASE WHEN SUM(sin_informacion) = 0 THEN 'completo' ELSE 'incompleto' END AS dia,
               SUM(transito), SUM(flujo_sin_transito),
               CASE WHEN SUM(sin_informacion) = 0
                    THEN ROUND(100.0 * SUM(transacciones) / NULLIF(SUM(ingresos) - SUM(transito), 0), 1) END
                    AS conversion_sin_transito,
               SUM(medidos), SUM(estimados), SUM(sin_informacion)
        FROM v_conversion_dia
        WHERE tipo_dia = :tipo
          AND fecha BETWEEN :desde AND :hasta
        GROUP BY tipo_dia
        """, nativeQuery = true)
    List<Object[]> filaDelTipo(@Param("tipo") String tipo,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    @Query(value = """
        SELECT id_acceso, fecha, id_bloque, ultima_sesion_valida, dias_sin_medir, semanas_sin_medir
        FROM v_pendiente_medir
        WHERE fecha = :fecha
        ORDER BY id_bloque, id_acceso
        """, nativeQuery = true)
    List<Object[]> pendientesDeMedir(@Param("fecha") LocalDate fecha);
}
