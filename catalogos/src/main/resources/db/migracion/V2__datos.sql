-- Rincón Sur · migración Flyway (MySQL 8)
-- Generado desde sql/sqlite/*.sql el 16-11-2026. Sin claves foráneas entre bases.

-- 4 accesos, 11 bloques, 3 clasificaciones, 5 rangos y 4 compañías

INSERT INTO acceso (id_acceso, codigo, nombre, ubicacion, observacion) VALUES
  (1, 'A1', 'Acceso Principal', 'Frente al pasillo central del mall', 'Entrada natural de quienes vienen del mall'),
  (2, 'A2', 'Acceso Lateral', 'Costado del sector patio de comidas', 'Flujo alto a la hora de almuerzo'),
  (3, 'A3', 'Acceso Estacionamiento', 'Conecta con el estacionamiento subterraneo', 'Zona con senal de red debil'),
  (4, 'A4', 'Acceso Conexión', 'Comunica con la tienda vecina de mejoramiento del hogar', 'Principal punto de transito entre la tienda vecina y el mall');

INSERT INTO bloque_horario (id_bloque, hora_inicio, hora_fin) VALUES
  (1, '10:00:00', '10:59:59'),
  (2, '11:00:00', '11:59:59'),
  (3, '12:00:00', '12:59:59'),
  (4, '13:00:00', '13:59:59'),
  (5, '14:00:00', '14:59:59'),
  (6, '15:00:00', '15:59:59'),
  (7, '16:00:00', '16:59:59'),
  (8, '17:00:00', '17:59:59'),
  (9, '18:00:00', '18:59:59'),
  (10, '19:00:00', '19:59:59'),
  (11, '20:00:00', '20:59:59');

INSERT INTO clasificacion (id_clasificacion, nombre) VALUES
  (1, 'Tránsito'),
  (2, 'Visita'),
  (3, 'No determinado');

INSERT INTO rango_etario (id_rango, nombre, edad_min, edad_max, orden) VALUES
  (1, 'Menor de 18 años', 0, 17, 1),
  (2, 'De 18 a 25 años', 18, 25, 2),
  (3, 'De 26 a 40 años', 26, 40, 3),
  (4, 'De 41 a 60 años', 41, 60, 4),
  (5, '61 años o más', 61, NULL, 5);

INSERT INTO tipo_compania (id_tipo_compania, nombre) VALUES
  (1, 'Solo'),
  (2, 'Pareja'),
  (3, 'Con niños'),
  (4, 'En grupo de 3 o más');
