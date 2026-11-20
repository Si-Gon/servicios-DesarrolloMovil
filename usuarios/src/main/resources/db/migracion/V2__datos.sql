-- Rincón Sur · migración Flyway (MySQL 8)
-- Generado desde sql/sqlite/*.sql el 16-11-2026. Sin claves foráneas entre bases.

-- 4 roles, 6 preguntas de seguridad y 7 usuarios con su hash

INSERT INTO rol (id_rol, codigo, nombre, descripcion) VALUES
  (1, 'P-01', 'Jefa de tienda', 'Consulta indicadores, historial y perfil; registra la meta'),
  (2, 'P-02', 'Subgerente comercial', 'Administra sesiones, valida, carga ingresos y transacciones, gestiona colaboradores'),
  (3, 'P-03', 'Supervisor de piso', 'Programa y valida sesiones, registra eventos, puede observar'),
  (4, 'P-04', 'Colaborador observador', 'Registra y anula sus propios registros en sesion en curso'),
  (5, 'P-05', 'Administradora', 'Crea y edita las cuentas de la tienda y entrega el codigo temporal');

INSERT INTO usuario (id_usuario, id_rol, rut, nombres, apellido_paterno, apellido_materno, cargo, correo, telefono, password_hash, activo) VALUES
  (1, 1, '15.234.871-1', 'Carolina', 'Fuentes', 'Araya', 'Jefa de tienda', 'carolina.fuentes@rinconsur-ficticio.ejemplo', '+56 9 0000 0011', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (2, 2, '13.987.442-0', 'Rodrigo', 'Saavedra', 'Munoz', 'Subgerente comercial', 'rodrigo.saavedra@rinconsur-ficticio.ejemplo', '+56 9 0000 0012', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (3, 3, '17.654.320-5', 'Daniela', 'Pizarro', 'Toro', 'Supervisora de piso (turno manana)', 'daniela.pizarro@rinconsur-ficticio.ejemplo', '+56 9 0000 0013', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (4, 3, '16.443.019-7', 'Matias', 'Contreras', 'Vidal', 'Supervisor de piso (turno tarde)', 'matias.contreras@rinconsur-ficticio.ejemplo', '+56 9 0000 0014', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (5, 4, '19.876.345-4', 'Tamara', 'Rojas', 'Espinoza', 'Vendedora, Vestuario Mujer', 'tamara.rojas@rinconsur-ficticio.ejemplo', '+56 9 0000 0015', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (6, 4, '20.145.678-9', 'Felipe', 'Ibarra', 'Soto', 'Vendedor, Hogar y Deco', 'felipe.ibarra@rinconsur-ficticio.ejemplo', '+56 9 0000 0016', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (7, 4, '18.322.904-4', 'Javiera', 'Morales', 'Caceres', 'Cajera, Cajas Centrales', 'javiera.morales@rinconsur-ficticio.ejemplo', '+56 9 0000 0017', '$2a$10$semilla.demo.no.usar.en.produccion', 1),
  (8, 5, '20.543.219-1', 'Paula', 'Contreras', 'Silva', 'Administradora', 'paula.contreras@rinconsur-ficticio.ejemplo', '+56 9 0000 0018', '$2a$10$semilla.demo.no.usar.en.produccion', 1);

UPDATE usuario SET password_hash = 'e62b302fc482e6b882a258d3e32765076af609b882f0c98f493ebe1c0dddf8eb' WHERE rut = '13.987.442-0';

UPDATE usuario SET password_hash = 'c72033a4a579c24fa24ca3fcfae9314da208be61b33c5e85c15a9adc6ad2217b' WHERE rut = '15.234.871-1';

UPDATE usuario SET password_hash = '297de3e3c3510021fe722754a64a9a80d81b36f2f716d380c62f276c2d88ca5f' WHERE rut = '16.443.019-7';

UPDATE usuario SET password_hash = '606b61a6db5c4ec29f240888f40de8e47f367e6bf0aad85ff9dac911156bed6b' WHERE rut = '17.654.320-5';

UPDATE usuario SET password_hash = '3be196465177bea9d42580f65fd1b71b26aca25fed5e2346470148c727bcbd00' WHERE rut = '18.322.904-4';

UPDATE usuario SET password_hash = 'e1553d2b50bf5930127ad34bc182803cf351ae61de120a71d85da0fa2a1c78f1' WHERE rut = '19.876.345-4';

UPDATE usuario SET password_hash = '640c5d498a480b562da0e3bd40334b2df49a24ddca1b41f9e52449534668d5ee' WHERE rut = '20.145.678-9';

-- Género de cada persona: el cargo se escribe según corresponda.
UPDATE usuario SET genero = 'F' WHERE rut IN ('15.234.871-1', '17.654.320-5', '19.876.345-4', '18.322.904-4', '20.543.219-1');
UPDATE usuario SET genero = 'M' WHERE rut IN ('13.987.442-0', '16.443.019-7', '20.145.678-9');

UPDATE usuario SET password_hash = '132948b7130fa027beaac82a2da35fa0e20f1f640973ec00d71430e0cc278aa1' WHERE rut = '20.543.219-1';

INSERT INTO pregunta_seguridad (id_pregunta, texto) VALUES
  (1, '¿Cuál es el nombre de tu primera mascota?'),
  (2, '¿En qué ciudad nació tu madre?'),
  (3, '¿Cuál fue tu primer colegio?'),
  (4, '¿Cuál fue tu primer trabajo?'),
  (5, '¿Cuál es tu comida favorita?'),
  (6, '¿Cómo se llamaba tu mejor amigo de la infancia?');
