package com.rinconsur.medicion.repository;

import com.rinconsur.medicion.model.SesionObservacion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

// El reporte se arma con las mismas vistas que los indicadores: el servicio no recalcula nada
// por su cuenta, así la app y el reporte nunca muestran números distintos del mismo día.
// Los nombres de rangos y compañías están en el servicio de catálogos: acá se agrupa por id.
public interface ReporteRepositorio extends Repository<SesionObservacion, Integer> {

    @Query(value = "SELECT porcentaje FROM meta_conversion WHERE anio = :anio AND mes = :mes",
           nativeQuery = true)
    List<Object> metaDelMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query(value = """
        SELECT COUNT(*) AS dias,
               ROUND(100.0 * SUM(transacciones) / NULLIF(SUM(ingresos), 0), 1) AS conversion
        FROM v_conversion_dia
        WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo
        """, nativeQuery = true)
    List<Object[]> avanceDelMes(@Param("periodo") String periodo);

    @Query(value = """
        SELECT DATE_FORMAT(fecha, '%Y-%m') AS mes, COALESCE(SUM(ingresos),0), COALESCE(SUM(transacciones),0),
               ROUND(100.0 * SUM(transacciones) / NULLIF(SUM(ingresos), 0), 1), COUNT(*)
        FROM v_conversion_dia
        GROUP BY DATE_FORMAT(fecha, '%Y-%m')
        ORDER BY mes DESC
        LIMIT 6
        """, nativeQuery = true)
    List<Object[]> meses();

    @Query(value = "SELECT DATE_FORMAT(MAX(fecha), '%Y-%m') FROM v_conversion_dia", nativeQuery = true)
    String ultimoMesConDatos();

    @Query(value = """
        SELECT id_bloque, COALESCE(SUM(ingresos), 0), COALESCE(SUM(transito_estimado), 0),
               SUM(CASE WHEN origen = 'medido' THEN 1 ELSE 0 END), COUNT(*)
        FROM v_transito_bloque
        WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo
        GROUP BY id_bloque
        ORDER BY id_bloque
        """, nativeQuery = true)
    List<Object[]> horas(@Param("periodo") String periodo);

    @Query(value = """
        SELECT CAST(id_rango AS CHAR) AS etiqueta, SUM(personas) AS cantidad,
               ROUND(100.0 * SUM(personas) / NULLIF((SELECT SUM(personas) FROM v_perfil_transito WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo), 0), 1)
        FROM v_perfil_transito
        WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo
        GROUP BY id_rango
        ORDER BY cantidad DESC
        """, nativeQuery = true)
    List<Object[]> porRango(@Param("periodo") String periodo);

    @Query(value = """
        SELECT CAST(id_tipo_compania AS CHAR) AS etiqueta, SUM(personas) AS cantidad,
               ROUND(100.0 * SUM(personas) / NULLIF((SELECT SUM(personas) FROM v_perfil_transito WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo), 0), 1)
        FROM v_perfil_transito
        WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo
        GROUP BY id_tipo_compania
        ORDER BY cantidad DESC
        """, nativeQuery = true)
    List<Object[]> porCompania(@Param("periodo") String periodo);

    @Query(value = """
        SELECT CASE WHEN compras_otra_tienda = 1 THEN 'si' ELSE 'no' END AS etiqueta,
               SUM(personas) AS cantidad,
               ROUND(100.0 * SUM(personas) / NULLIF((SELECT SUM(personas) FROM v_perfil_transito WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo), 0), 1)
        FROM v_perfil_transito
        WHERE DATE_FORMAT(fecha, '%Y-%m') = :periodo
        GROUP BY compras_otra_tienda
        ORDER BY compras_otra_tienda DESC
        """, nativeQuery = true)
    List<Object[]> compras(@Param("periodo") String periodo);
}
