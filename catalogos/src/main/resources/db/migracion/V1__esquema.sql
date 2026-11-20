-- Rincón Sur · migración Flyway (MySQL 8)
-- Generado desde sql/sqlite/*.sql el 16-11-2026. Sin claves foráneas entre bases.

CREATE TABLE acceso (
    id_acceso   INT          NOT NULL,
    codigo      CHAR(2)      NOT NULL,
    nombre      VARCHAR(40)  NOT NULL,
    ubicacion   VARCHAR(120) NOT NULL,
    observacion VARCHAR(200) NULL,
    CONSTRAINT pk_acceso PRIMARY KEY (id_acceso),
    CONSTRAINT uq_acceso_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bloque_horario (
    id_bloque   INT  NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin    TIME NOT NULL,
    CONSTRAINT pk_bloque_horario PRIMARY KEY (id_bloque),
    CONSTRAINT uq_bloque_inicio UNIQUE (hora_inicio),
    CONSTRAINT ck_bloque_orden CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clasificacion (
    id_clasificacion INT         NOT NULL,
    nombre           VARCHAR(30) NOT NULL,
    CONSTRAINT pk_clasificacion PRIMARY KEY (id_clasificacion),
    CONSTRAINT uq_clasificacion_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rango_etario (
    id_rango INT         NOT NULL,
    nombre   VARCHAR(30) NOT NULL,
    edad_min INT         NOT NULL,
    edad_max INT         NULL,
    orden    INT         NOT NULL,
    CONSTRAINT pk_rango PRIMARY KEY (id_rango),
    CONSTRAINT uq_rango_nombre UNIQUE (nombre),
    CONSTRAINT uq_rango_orden UNIQUE (orden),
    CONSTRAINT ck_rango_edad CHECK (edad_max IS NULL OR edad_max >= edad_min)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tipo_compania (
    id_tipo_compania INT         NOT NULL,
    nombre           VARCHAR(40) NOT NULL,
    CONSTRAINT pk_tipo_compania PRIMARY KEY (id_tipo_compania),
    CONSTRAINT uq_tipo_compania_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
