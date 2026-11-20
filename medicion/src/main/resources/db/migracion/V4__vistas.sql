-- Rincón Sur · vistas del servicio medicion (MySQL 8)
-- SQL del caso, adaptado: los joins a catálogos se reemplazan por la llave que el hecho guarda.

CREATE VIEW v_tipo_dia AS
SELECT d.fecha,
       CASE WHEN DAYOFWEEK(d.fecha) IN (1, 7)
                 OR EXISTS (SELECT 1 FROM evento_dia e
                            JOIN tipo_evento t ON t.id_tipo_evento = e.id_tipo_evento
                            WHERE t.nombre = 'Feriado' AND e.anulado = 0
                              AND d.fecha BETWEEN e.fecha_inicio AND e.fecha_fin)
            THEN 'Fin de semana' ELSE 'Habil' END AS tipo_dia
FROM (SELECT fecha FROM conteo_ingreso UNION SELECT fecha FROM sesion_observacion) d;

CREATE VIEW v_sesion_proporcion AS
SELECT s.id_sesion, s.codigo, s.id_acceso, s.id_bloque, s.fecha, e.nombre AS estado,
       SUM(CASE WHEN r.anulado = 0 THEN 1 ELSE 0 END) AS registros_validos,
       SUM(CASE WHEN r.anulado = 0 AND r.id_clasificacion = 1 THEN 1 ELSE 0 END) AS transito,
       SUM(CASE WHEN r.anulado = 0 AND r.id_clasificacion = 2 THEN 1 ELSE 0 END) AS visita,
       SUM(CASE WHEN r.anulado = 0 AND r.id_clasificacion = 3 THEN 1 ELSE 0 END) AS no_determinado,
       ROUND(100.0 * SUM(CASE WHEN r.anulado = 0 AND r.id_clasificacion = 1 THEN 1 ELSE 0 END)
             / NULLIF(SUM(CASE WHEN r.anulado = 0 AND r.id_clasificacion <> 3 THEN 1 ELSE 0 END), 0), 1) AS pct_transito
FROM sesion_observacion s
JOIN estado_sesion e ON e.id_estado = s.id_estado
LEFT JOIN registro_observacion r ON r.id_sesion = s.id_sesion
GROUP BY s.id_sesion, s.codigo, s.id_acceso, s.id_bloque, s.fecha, e.nombre;

CREATE VIEW v_transito_bloque AS
SELECT ci.fecha, td.tipo_dia, ci.id_acceso, ci.id_bloque, ci.cantidad AS ingresos,
       MAX(CASE WHEN sv.fecha = ci.fecha THEN sv.pct_transito END) AS pct_sesion,
       ROUND(AVG(CASE WHEN sv.fecha < ci.fecha THEN sv.pct_transito END), 1) AS pct_promedio,
       CASE WHEN MAX(CASE WHEN sv.fecha = ci.fecha THEN sv.pct_transito END) IS NOT NULL THEN 'medido'
            WHEN AVG(CASE WHEN sv.fecha < ci.fecha THEN sv.pct_transito END) IS NOT NULL THEN 'estimado'
            ELSE 'sin información' END AS origen,
       ROUND(ci.cantidad * COALESCE(MAX(CASE WHEN sv.fecha = ci.fecha THEN sv.pct_transito END),
                                    AVG(CASE WHEN sv.fecha < ci.fecha THEN sv.pct_transito END)) / 100) AS transito_estimado
FROM conteo_ingreso ci
JOIN v_tipo_dia td ON td.fecha = ci.fecha
LEFT JOIN v_sesion_proporcion sv
       ON sv.id_acceso = ci.id_acceso AND sv.estado = 'Validada'
      AND ( (sv.fecha = ci.fecha AND sv.id_bloque = ci.id_bloque)
         OR (sv.fecha < ci.fecha AND sv.fecha >= DATE_SUB(ci.fecha, INTERVAL 28 DAY)
             AND EXISTS (SELECT 1 FROM v_tipo_dia t2 WHERE t2.fecha = sv.fecha AND t2.tipo_dia = td.tipo_dia)) )
GROUP BY ci.fecha, td.tipo_dia, ci.id_acceso, ci.id_bloque, ci.cantidad;

CREATE VIEW v_pendiente_medir AS
SELECT ci.id_acceso, ci.fecha, ci.id_bloque, ult.ultima AS ultima_sesion_valida,
       CASE WHEN ult.ultima IS NULL THEN NULL ELSE DATEDIFF(ci.fecha, ult.ultima) END AS dias_sin_medir,
       CASE WHEN ult.ultima IS NULL THEN NULL ELSE ROUND(DATEDIFF(ci.fecha, ult.ultima) / 7, 1) END AS semanas_sin_medir
FROM conteo_ingreso ci
LEFT JOIN (SELECT s.id_acceso, s.id_bloque, MAX(s.fecha) AS ultima
           FROM sesion_observacion s JOIN estado_sesion e ON e.id_estado = s.id_estado
           WHERE e.nombre = 'Validada' GROUP BY s.id_acceso, s.id_bloque) ult
       ON ult.id_acceso = ci.id_acceso AND ult.id_bloque = ci.id_bloque
WHERE NOT EXISTS (SELECT 1 FROM sesion_observacion s2 JOIN estado_sesion e2 ON e2.id_estado = s2.id_estado
                  WHERE s2.id_acceso = ci.id_acceso AND s2.fecha = ci.fecha
                    AND s2.id_bloque = ci.id_bloque AND e2.nombre = 'Validada');

CREATE VIEW v_perfil_transito AS
SELECT s.id_acceso, s.fecha, td.tipo_dia, r.id_rango, r.id_tipo_compania, r.compras_otra_tienda, COUNT(*) AS personas
FROM registro_observacion r
JOIN sesion_observacion s ON s.id_sesion = r.id_sesion
JOIN estado_sesion e ON e.id_estado = s.id_estado
LEFT JOIN v_tipo_dia td ON td.fecha = s.fecha
WHERE r.anulado = 0 AND e.id_estado = 4 AND r.id_clasificacion = 1
GROUP BY s.id_acceso, s.fecha, td.tipo_dia, r.id_rango, r.id_tipo_compania, r.compras_otra_tienda;

CREATE VIEW v_conversion_dia AS
SELECT i.fecha, td.tipo_dia, i.ingresos, t.transacciones,
       ROUND(100.0 * t.transacciones / NULLIF(i.ingresos, 0), 1) AS conversion_bruta,
       CASE WHEN b.bloques = 44 AND b.accesos = 4 AND tb.bloques_tx = 11 THEN 'completo' ELSE 'incompleto' END AS dia,
       tr.transito, i.ingresos - tr.transito AS flujo_sin_transito,
       CASE WHEN tr.sin_informacion = 0
            THEN ROUND(100.0 * t.transacciones / NULLIF(i.ingresos - tr.transito, 0), 1) END AS conversion_sin_transito,
       tr.medidos, tr.estimados, tr.sin_informacion
FROM (SELECT fecha, SUM(cantidad) AS ingresos FROM conteo_ingreso GROUP BY fecha) i
JOIN (SELECT fecha, SUM(cantidad) AS transacciones FROM transaccion_bloque GROUP BY fecha) t ON t.fecha = i.fecha
JOIN (SELECT fecha, COUNT(*) AS bloques, COUNT(DISTINCT id_acceso) AS accesos FROM conteo_ingreso GROUP BY fecha) b ON b.fecha = i.fecha
LEFT JOIN (SELECT fecha, COUNT(*) AS bloques_tx FROM transaccion_bloque GROUP BY fecha) tb ON tb.fecha = i.fecha
LEFT JOIN (SELECT fecha, SUM(transito_estimado) AS transito,
                  SUM(origen = 'medido') AS medidos, SUM(origen = 'estimado') AS estimados,
                  SUM(origen = 'sin información') AS sin_informacion
           FROM v_transito_bloque GROUP BY fecha) tr ON tr.fecha = i.fecha
LEFT JOIN v_tipo_dia td ON td.fecha = i.fecha;
