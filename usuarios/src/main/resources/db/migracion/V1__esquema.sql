-- Rincón Sur · migración Flyway (MySQL 8)
-- Generado desde sql/sqlite/*.sql el 16-11-2026. Sin claves foráneas entre bases.

CREATE TABLE rol (
    id_rol      INT          NOT NULL,
    codigo      VARCHAR(5)   NOT NULL,
    nombre      VARCHAR(40)  NOT NULL,
    descripcion VARCHAR(200) NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_codigo UNIQUE (codigo),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT ck_rol_codigo CHECK (codigo IN ('P-01','P-02','P-03','P-04','P-05'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE usuario (
    id_usuario INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_rol         INT          NOT NULL,
    rut            VARCHAR(12)  NOT NULL,
    nombres        VARCHAR(60)  NOT NULL,
    apellido_paterno VARCHAR(40) NOT NULL,
    apellido_materno VARCHAR(40) NULL,
    -- El cargo se escribe según la persona (jefa o jefe), así que se guarda el género.
    genero         CHAR(1)      NOT NULL DEFAULT 'F',
    cargo          VARCHAR(60)  NOT NULL,
    correo         VARCHAR(100) NOT NULL,
    telefono       VARCHAR(20)  NULL,
    password_hash  VARCHAR(255) NOT NULL,
    -- La cuenta nace con un código temporal del administrador: mientras esté en 1, el login
    -- no deja entrar sin cambiar la clave y definir la pregunta de seguridad.
    debe_cambiar_clave BOOLEAN  NOT NULL DEFAULT 0,
    codigo_vence   DATETIME     NULL,
    activo         BOOLEAN      NOT NULL DEFAULT 1,
    foto           LONGBLOB         NULL,
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_rut UNIQUE (rut),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_genero CHECK (genero IN ('F','M')),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pregunta_seguridad (
    id_pregunta INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    texto       VARCHAR(120) NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT 1,
    CONSTRAINT uq_pregunta_texto UNIQUE (texto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE respuesta_seguridad (
    id_usuario     INT          NOT NULL,
    id_pregunta    INT          NOT NULL,
    respuesta_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_respuesta_seguridad PRIMARY KEY (id_usuario, id_pregunta),
    CONSTRAINT fk_respuesta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_respuesta_pregunta FOREIGN KEY (id_pregunta) REFERENCES pregunta_seguridad (id_pregunta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
