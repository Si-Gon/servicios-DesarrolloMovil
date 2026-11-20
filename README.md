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

Todo lo que sigue se pide **por el gateway** (`http://localhost:8090`).

### Swagger

Una sola página con las tres APIs: **http://localhost:8090/swagger-ui.html**. El selector de arriba
cambia entre Catálogos, Usuarios y Medición; el gateway pide cada documento al servicio dueño por sus
propias rutas (`/docs/<servicio>/v3/api-docs`), así que no hay que abrir tres pestañas ni pelear con
CORS. Cada servicio sigue teniendo además el suyo en `http://localhost:809x/swagger-ui.html`.

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
| POST | `/api/sesiones` | Programa una sesión; el bloque se deduce de la hora de inicio |
| PATCH | `/api/sesiones/{id}/iniciar` | Marca el inicio real |
| PATCH | `/api/sesiones/{id}/cerrar` | Cierra; exige el mínimo de 30 registros |
| PATCH | `/api/sesiones/{id}/validar` | Valida una sesión cerrada |
| PATCH | `/api/sesiones/{id}/rechazar` | Rechaza con motivo escrito |
| POST | `/api/registros` | Anota una persona en una sesión en curso |
| PATCH | `/api/registros/{id}/anular` | Anula un registro con su motivo, sin borrarlo |
| GET | `/api/registros/sesion/{id}/ultimos` | Los últimos diez registros válidos |
| GET | `/api/indicadores/{fecha}` | Conversión y tránsito del día, leídos de la vista |
| GET | `/api/indicadores?tipoDia=Habil` | Agregado por tipo de día, con `desde` y `hasta` opcionales |
| GET | `/api/indicadores/pendientes/{fecha}` | Bloques con ingresos cargados sin sesión validada |

## Las reglas de la sesión

El ciclo de una sesión está escrito en el servicio, no en la app: la app solo toca botones.

```
Programada → En curso → Finalizada → Validada
                                 ↘ Rechazada (con motivo)
```

- **El bloque no se elige**: se deduce de la hora de inicio contra el catálogo de `catalogos`, y el
  acceso se valida contra el mismo servicio. Es la única llamada entre servicios del proyecto, y es
  de lectura, con caché de 60 segundos para no preguntar en cada toque.
- **Cerrar exige 30 registros válidos.** Si no llega, responde `400` diciendo cuántos lleva.
- **Nadie valida ni rechaza su propia sesión**: responde `403` y la regla también está en la tabla.
- **Cada operación exige el estado anterior.** Si la sesión cambió entre que la pantalla se abrió y
  se tocó el botón, la respuesta es `409` con el estado real («Ahora está en «En curso»»).
- **Anular no borra**: deja la marca, quién la hizo, cuándo y por qué. El registro sigue en la base
  y deja de contar para los indicadores.
- **Quién anota sale del token.** El gateway pasa `X-Usuario-Id`; el servicio compara contra el
  observador de la sesión y responde `403` si es otra persona.

## Conectar la app móvil

La app apunta al **gateway** (8090), no a los servicios. La forma más simple de probar desde el
emulador o desde el teléfono por USB es el túnel de `adb`, que no depende del firewall de Windows
ni de que los dos equipos estén en la misma red:

```bash
adb reverse tcp:8090 tcp:8090
# en la app: Perfil → Probar la conexión, con http://localhost:8090
```

Y así queda la prueba de la pantalla de conexión, con el backend corriendo en el PC:

```
Inicio de sesión     Carolina Fuentes · P-01 · token de 237 caracteres      2282 ms
Catálogos            4 accesos · A1, A2, A3, A4                             494 ms
Sesiones del 15-09   2 sesiones · S-0001 Validada · S-0002 Validada         360 ms
Indicadores 15-09    5735 ingresos · 497 transacciones · 8.7 % conversión   315 ms
Conexión completa: la app habló con el gateway y con los tres servicios.
```

**Por qué el túnel y no la IP del PC**: con el emulador, `http://10.0.2.2:8090` es el PC visto desde
adentro, pero el firewall de Windows suele bloquear ese TCP entrante —el `ping` responde y la
conexión se queda esperando hasta el timeout—. `adb reverse` crea un túnel por el cable y evita el
problema. Si se prueba por la red local (teléfono en Wi-Fi, sin cable), hay que permitir el rango
en el firewall y usar la IP del PC en la pantalla de conexión:

```powershell
netsh advfirewall firewall add rule name="RinconSur API" dir=in action=allow protocol=TCP localport=8090-8093
```

La aplicación guarda la última dirección usada, así que no hay que escribirla cada vez.

## Seguridad

El token lo firma `usuarios` (HS256, 8 h, con el perfil dentro) y lo valida el **gateway** antes de
rutear: los servicios de negocio no se tocan. La política se lee en dos métodos de
`gateway/seguridad/PoliticaDeAcceso`, no repartida por las rutas.

| Nivel | Rutas |
|---|---|
| Público | `POST /api/auth/login`, `GET /api/auth/preguntas`, la documentación y `/actuator/**` |
| Con sesión | todo lo demás: catálogos, sesiones, indicadores y `GET /api/usuarios/{id}` (la ficha propia) |
| Solo administración (P-05) | `GET /api/usuarios` (la lista completa) y cualquier escritura sobre `/api/usuarios/**` |

Cuando el token es válido, el gateway agrega `X-Usuario-Id`, `X-Usuario-Rol` y `X-Usuario-Rut` a la
petición, así el servicio dueño sabe quién pide sin volver a consultar `usuarios`.

Lo que se comprobó por el gateway:

| Caso | Resultado |
|---|---|
| `GET /api/catalogos/accesos` sin token | `401` |
| `GET /api/catalogos/accesos` con token de jefa | `200` |
| `GET /api/usuarios` con token de jefa (P-01) | `403` |
| `GET /api/usuarios` con token de administración (P-05) | `200` |
| `GET /api/usuarios/1` con token de jefa | `200` (es una ficha, no la lista) |
| `POST /api/auth/login` | `200` (público) |
| token con la firma alterada | `401` |
| `Authorization` sin el prefijo `Bearer ` | `401` |
| servicio de medición abajo, pedido con token | `503` con motivo |

**Límite conocido**: el filtro vive en el borde, así que quien llame directo al puerto del servicio
(8091 a 8093) no pasa por él. Es una decisión del proyecto, no un olvido.

## Errores

Los tres servicios devuelven el mismo cuerpo —`{"motivo": "..."}`, con tildes— y el mismo juego de
estados. Cada uno tiene un solo `excepcion/ManejadorDeErrores`: si hubiera dos consejos, el formato
dependería del orden en que Spring los elija.

| Situación | Estado | Ejemplo de motivo |
|---|---|---|
| Un dato del `@Valid` no cumple | `400` | «El correo no tiene un formato válido.» |
| El RUT no pasa el módulo 11 | `400` | «El RUT no es válido.» |
| El cuerpo no es JSON válido | `400` | «El cuerpo de la petición no es válido.» |
| Un parámetro con el tipo equivocado | `400` | «Alguno de los datos enviados no es válido.» |
| El recurso no existe | `404` | «No existe una sesión con ese identificador.» |
| Choca con una regla de unicidad | `409` | «Ya existe una cuenta con el RUT 15.234.871-1.» |
| Lo inesperado | `500` | texto corto; el detalle queda en el log del servicio |

Las reglas del negocio se lanzan con fábricas de `ErrorDeNegocio` (`invalido`, `noEncontrado`,
`conflicto`, `noAutorizado`, `prohibido`), así el controlador queda con su camino feliz y una línea
para el error, sin armar `ResponseEntity` a mano en cada caso.

En el **gateway** el `401`, `403` y `503` los escribe el filtro; el `404` de una ruta sin dueño lo
formatea `excepcion/ManejadorDeErroresWeb`, porque en WebFlux los errores no pasan por un
`@RestControllerAdvice`. Detalle: una ruta inexistente responde `404` aunque no haya token, porque el
filtro corre dentro del ruteo y esa petición no llega a ninguna ruta.

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
  transacciones y 8,7 % de conversión** del 15-09-2026;
- el gateway valida el token, aplica la política por perfil y responde `401`, `403` y `503` con
  `{"motivo": "..."}`;
- los tres servicios responden `400`, `404`, `409` y `500` con el mismo cuerpo y los mismos textos;
- el ciclo completo de una sesión funciona por el gateway: programar (`201`, bloque deducido),
  iniciar, anotar 32 registros, cerrar, rechazar la validación propia (`403`) y validar con otra
  persona (`200`), con las filas correspondientes en MySQL;
- el Swagger del gateway reúne las tres APIs en una sola página.

Pendiente: la carga de la tienda (conteos por bloque y transacciones, que es lo que alimenta la
conversión) y llevar la conexión a las pantallas de la app, que hoy siguen leyendo SQLite.
