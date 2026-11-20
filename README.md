# Rincón Sur · servicios

Backend del caso **Rincón Sur (DSY1105, Evaluación Parcial 3)**: tres servicios de negocio en
Java Spring Boot con MySQL y dos de soporte —registro y puerta de entrada— para que la app móvil
en Kotlin consuma un solo origen.

La app no conoce puertos: pide todo al **gateway** y él resuelve a qué servicio va.

## Módulos

| Módulo | Tipo | Responsabilidad | Puerto | Base de datos |
|---|---|---|---|---|
| `eureka` | soporte | Registro y descubrimiento | 8761 | — |
| `gateway` | soporte | Puerta única; reparte `/api/**` | 8090 | — |
| `catalogos` | negocio | Datos maestros: accesos, bloques, clasificaciones, rangos y compañías | 8091 | `rinconsur_catalogos` |
| `usuarios` | negocio | Cuentas, roles, inicio de sesión y preguntas de seguridad | 8092 | `rinconsur_usuarios` |
| `medicion` | negocio | Sesiones, registros, eventos, carga de la tienda e indicadores | 8093 | `rinconsur_medicion` |

**No se usa el 8080**: en el computador del equipo lo ocupa Docker. Los servicios de negocio van
del 8091 al 8093 y el gateway en 8090; eureka mantiene el puerto por defecto de la pieza.

Cada base es de su dueño: no hay claves foráneas entre bases. Las columnas que apuntan a otro
servicio (`id_acceso`, `id_bloque`, `id_observador`) son columnas simples, y por eso
`sesion_observacion` guarda `observador_nombre`: el nombre de la persona vive en `usuarios` y la
lista de sesiones se lee muchas veces al día.

## Cómo se levanta

Requisito: **JDK 21**, **Maven** y **MySQL** prendido (el usuario de MySQL tiene que poder crear
bases). Las tres bases se crean solas en el primer arranque, con sus tablas y sus datos: en la URL
del datasource va `createDatabaseIfNotExist=true` y Flyway aplica las migraciones en el mismo
arranque.

```bash
mvn clean package -DskipTests          # compila los cinco módulos
java -jar eureka/target/eureka-1.0.0.jar          # 1° el registro
java -jar catalogos/target/catalogos-1.0.0.jar    # 2° los servicios de negocio
java -jar usuarios/target/usuarios-1.0.0.jar
java -jar medicion/target/medicion-1.0.0.jar
java -jar gateway/target/gateway-1.0.0.jar        # 3° la puerta
```

El orden importa solo para la demo: el gateway resuelve por nombre contra Eureka y cachea la
lista unos segundos, así que un servicio recién levantado puede responder `503` en el primer
intento. Los servicios de negocio arrancan aunque eureka esté abajo (la app los alcanza directo,
pero el gateway no los ve).

La conexión se ajusta por variables de entorno, sin editar archivos versionados:

| Variable | Valor por defecto |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(vacío)* |
| `DB_NAME` | el de cada servicio |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |
| `TOKEN_CLAVE` | clave de demo del `application.properties` de `usuarios` |

## Endpoints

Todo lo que sigue se pide **por el gateway** (`http://localhost:8090`). Cada servicio tiene su
Swagger en `/swagger-ui.html` y su documento en `/v3/api-docs`.

### Catálogos (8091)

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/catalogos/accesos` | Los cuatro accesos de la tienda (A1 a A4) |
| GET | `/api/catalogos/accesos/{codigo}` | Un acceso por su código |
| GET | `/api/catalogos/bloques` | Los once bloques horarios |
| GET | `/api/catalogos/clasificaciones` | Visita, tránsito y no determinado |
| GET | `/api/catalogos/rangos` | Rangos etarios en orden |
| GET | `/api/catalogos/companias` | Solo, pareja, familia y grupo |

### Usuarios (8092)

| Método | Ruta | Qué devuelve |
|---|---|---|
| POST | `/api/auth/login` | `{token, usuario}`; el token lleva el perfil dentro |
| GET | `/api/auth/preguntas` | Preguntas de seguridad activas |
| GET | `/api/usuarios` | Las cuentas, por perfil y nombre |
| GET | `/api/usuarios/{id}` | Una cuenta |
| GET | `/api/usuarios/roles` | Los cinco perfiles del caso |
| POST | `/api/usuarios` | Crea la cuenta y devuelve el **código temporal** |

### Medición (8093)

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/sesiones?fecha=AAAA-MM-DD` | Sesiones de una fecha (por defecto, hoy) |
| GET | `/api/sesiones/por-validar` | Cerradas por el observador, esperando revisión |
| GET | `/api/sesiones/{id}` | Una sesión con su estado y sus registros |
| GET | `/api/indicadores/{fecha}` | Conversión y tránsito del día, leídos de la vista |
| GET | `/api/indicadores?tipoDia=Habil` | Agregado por tipo de día, con `desde` y `hasta` opcionales |
| GET | `/api/indicadores/pendientes/{fecha}` | Bloques con ingresos cargados sin sesión validada |

## Migraciones

Viven en `src/main/resources/db/migracion`, con `V1__`, `V2__` … y **una migración aplicada no se
edita**: el cambio va en la siguiente.

| Servicio | Migraciones | Contenido |
|---|---|---|
| `catalogos` | `V1__esquema`, `V2__datos` | 5 tablas y sus datos maestros |
| `usuarios` | `V1__esquema`, `V2__datos` | 4 tablas, 5 roles, 8 cuentas, 6 preguntas |
| `medicion` | `V1__esquema`, `V2__datos`, `V3__demo_sabado`, `V4__vistas` | 8 tablas, el anexo del 15-09, el sábado 24-10 y las 6 vistas |

`spring.jpa.hibernate.ddl-auto=validate`: el esquema lo manda Flyway y, si la entidad y la tabla
no calzan, el servicio no arranca. Es la red de seguridad del modelo.

## Cuentas para probar

Todas las cuentas sembradas usan el formato de clave de la app —`sha256("RUT|clave")`— y la clave
`rincon2026`. Las cuentas nuevas guardan **PBKDF2 con sal**; el servicio acepta los dos formatos.

| RUT | Persona | Perfil |
|---|---|---|
| 20.543.219-1 | Paula Contreras | P-05 Administradora |
| 15.234.871-1 | Carolina Fuentes | P-01 Jefa de tienda |
| 13.987.442-0 | Rodrigo Saavedra | P-02 Subgerente comercial |
| 17.654.320-5 | Daniela Pizarro | P-03 Supervisora de piso |
| 19.876.345-4 | Tamara Rojas | P-04 Colaboradora observadora |

```bash
curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"rut":"15.234.871-1","clave":"rincon2026"}'
```

## Decisiones que sostienen el diseño

- **Los indicadores los calcula la base.** El servicio lee las vistas `v_conversion_dia`,
  `v_transito_bloque` y `v_pendiente_medir`; no reimplementa la fórmula en Java. Si la app y el
  servicio usaran fórmulas distintas, dos pantallas mostrarían números distintos del mismo día.
- **El caso completo en un servicio.** Las seis vistas cruzan conteos, sesiones y eventos: repartir
  eso obliga a reconstruir el cruce por HTTP o a duplicar filas.
- **Los eventos viven con la medición.** Un evento bloquea un acceso o un bloque, y ese bloqueo es
  condición para escribir un registro: preguntarlo por red en el pasillo, donde la persona está
  contando gente, no tiene sentido.
- **Las credenciales son la única frontera que no comparte datos.** El rol viaja en el token, así
  que los demás servicios autorizan sin llamar a `usuarios`.
- **Código y mensajes en castellano.** Los errores salen siempre con la misma forma,
  `{"motivo": "..."}`, y el texto está escrito para quien lo va a leer en la app.
- **Sin servidor de configuración.** Se evaluó y se descartó: con cinco módulos, los `.properties`
  por servicio son más simples de seguir que un `config` aparte que hay que levantar primero.
- **CORS solo en el gateway.** Si el servicio también lo declara, la respuesta sale con dos
  cabeceras y el navegador la rechaza.

## Estado

Funcionando y verificado contra MySQL 8.4:

- los cinco módulos compilan (`mvn clean package`);
- las tres bases se crean solas y Flyway aplica 2, 2 y 4 migraciones;
- los cuatro servicios se registran en Eureka;
- los endpoints responden por el gateway, con las cifras del anexo: **5.735 ingresos, 497
  transacciones y 8,7 % de conversión** del 15-09-2026.

Pendiente: escritura de la medición (programar, cerrar y validar sesiones, registrar y anular,
cargar conteos), el filtro que valida el token en el gateway y la conexión de la app con Retrofit.
