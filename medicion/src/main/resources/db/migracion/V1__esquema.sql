-- Rincón Sur · migración Flyway (MySQL 8)
-- Generado desde sql/sqlite/*.sql el 16-11-2026. Sin claves foráneas entre bases.

CREATE TABLE estado_sesion (
    id_estado INT         NOT NULL,
    nombre    VARCHAR(30) NOT NULL,
    CONSTRAINT pk_estado_sesion PRIMARY KEY (id_estado),
    CONSTRAINT uq_estado_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tipo_evento (
    id_tipo_evento INT         NOT NULL,
    nombre         VARCHAR(40) NOT NULL,
    CONSTRAINT pk_tipo_evento PRIMARY KEY (id_tipo_evento),
    CONSTRAINT uq_tipo_evento_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sesion_observacion (
    id_sesion INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(10)  NOT NULL,
    id_acceso       INT          NOT NULL,
    fecha           DATE         NOT NULL,
    hora_inicio_prog TIME        NOT NULL,
    hora_fin_prog    TIME        NOT NULL,
    inicio_real     DATETIME     NULL,
    fin_real        DATETIME     NULL,
    id_estado       INT          NOT NULL,
    id_bloque       INT          NOT NULL,
    -- Copia del nombre del observador: las personas viven en el servicio de usuarios y esta
    -- pantalla se lee muchas veces al día. Es una foto del momento, no una segunda fuente.
    observador_nombre VARCHAR(120) NULL,   -- bloque horario de la sesión,
    id_observador   INT          NOT NULL,
    id_programador  INT          NOT NULL,
    id_revisor      INT          NULL,
    fecha_revision  DATETIME     NULL,
    motivo_rechazo  VARCHAR(200) NULL,
    CONSTRAINT uq_sesion_codigo UNIQUE (codigo),
    CONSTRAINT ck_sesion_orden CHECK (hora_fin_prog > hora_inicio_prog),
    CONSTRAINT ck_sesion_revisor CHECK (id_revisor IS NULL OR id_revisor <> id_observador),
    CONSTRAINT ck_sesion_motivo CHECK (id_estado <> 5 OR motivo_rechazo IS NOT NULL),
    CONSTRAINT fk_sesion_estado FOREIGN KEY (id_estado) REFERENCES estado_sesion (id_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE registro_observacion (
    id_registro INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_sesion           INT          NOT NULL,
    fecha_hora          DATETIME     NOT NULL,
    id_clasificacion    INT          NOT NULL,
    id_rango            INT          NOT NULL,
    id_tipo_compania    INT          NOT NULL,
    compras_otra_tienda BOOLEAN      NOT NULL DEFAULT 0,
    id_acceso_destino   INT          NULL,
    anulado             BOOLEAN      NOT NULL DEFAULT 0,
    id_usuario_anula    INT          NULL,
    fecha_anulacion     DATETIME     NULL,
    motivo_anulacion    VARCHAR(200) NULL,
    CONSTRAINT ck_registro_anulacion CHECK (anulado = 0 OR (id_usuario_anula IS NOT NULL AND motivo_anulacion IS NOT NULL)),
    CONSTRAINT fk_registro_sesion FOREIGN KEY (id_sesion) REFERENCES sesion_observacion (id_sesion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE evento_dia (
    id_evento INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(6)   NOT NULL,
    fecha_inicio    DATE         NOT NULL,
    fecha_fin       DATE         NOT NULL,
    id_tipo_evento  INT          NOT NULL,
    id_acceso       INT          NULL,
    descripcion     VARCHAR(200) NOT NULL,
    id_usuario_registra INT      NOT NULL,
    fecha_registro  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    foto                LONGBLOB         NULL,
    anulado             BOOLEAN      NOT NULL DEFAULT 0,
    motivo_anulacion    VARCHAR(200) NULL,
    id_usuario_anula    INT          NULL,
    fecha_anulacion     DATETIME     NULL,
    CONSTRAINT uq_evento_codigo UNIQUE (codigo),
    CONSTRAINT ck_evento_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT fk_evento_tipo FOREIGN KEY (id_tipo_evento) REFERENCES tipo_evento (id_tipo_evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE conteo_ingreso (
    id_conteo INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- Nulo = el bloque se cargó como total del local, sin repartir entre accesos.
    id_acceso            INT      NULL,
    fecha                DATE     NOT NULL,
    id_bloque            INT      NOT NULL,
    cantidad             INT      NOT NULL,
    id_usuario_carga     INT      NOT NULL,
    fecha_carga          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_correccion INT     NULL,
    fecha_correccion     DATETIME NULL,
    -- MySQL no tiene índices únicos parciales: esta columna convierte el nulo en 0 para que
    -- el índice impida cargar dos veces el mismo bloque, con detalle o como total.
    id_acceso_clave      INT      AS (IFNULL(id_acceso, 0)) STORED,
    CONSTRAINT uq_conteo_bloque UNIQUE (id_acceso_clave, fecha, id_bloque),
    CONSTRAINT ck_conteo_cantidad CHECK (cantidad >= 0),
    CONSTRAINT ck_conteo_correccion CHECK ((id_usuario_correccion IS NULL) = (fecha_correccion IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE transaccion_bloque (
    id_transaccion INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha            DATE     NOT NULL,
    id_bloque        INT      NOT NULL,
    cantidad         INT      NOT NULL,
    id_usuario_carga INT      NOT NULL,
    fecha_carga      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_correccion INT     NULL,
    fecha_correccion DATETIME NULL,
    CONSTRAINT uq_transaccion_bloque UNIQUE (fecha, id_bloque),
    CONSTRAINT ck_transaccion_correccion CHECK ((id_usuario_correccion IS NULL) = (fecha_correccion IS NULL)),
    CONSTRAINT ck_transaccion_cantidad CHECK (cantidad >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE meta_conversion (
    id_meta INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anio               SMALLINT     NOT NULL,
    mes                TINYINT      NOT NULL,
    porcentaje         DECIMAL(5,2) NOT NULL,
    id_usuario_registra INT         NOT NULL,
    CONSTRAINT uq_meta_periodo UNIQUE (anio, mes),
    CONSTRAINT ck_meta_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_meta_porcentaje CHECK (porcentaje BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
