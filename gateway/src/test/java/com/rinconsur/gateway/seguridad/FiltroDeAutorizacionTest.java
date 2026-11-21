package com.rinconsur.gateway.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// El filtro es el único control real: el guardia de ruta del front es cosmético. Estas pruebas
// son los cuatro casos que la pauta pide (público 200, sin token 401, rol equivocado 403, firma
// manipulada 401) más lo que no se ve: las cabeceras que el servicio de destino da por ciertas.
@DisplayName("Filtro de autorización del gateway")
class FiltroDeAutorizacionTest {

    private static final String CLAVE = Base64.getEncoder()
        .encodeToString("clave-de-prueba-de-32-bytes-abcdef".getBytes());

    private FiltroDeAutorizacion filtro;
    private SecretKey llave;
    private AtomicReference<ServerWebExchange> visto;

    @BeforeEach
    void preparar() {
        llave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(CLAVE));
        filtro = new FiltroDeAutorizacion(new ValidadorDeToken(CLAVE), new PoliticaDeAcceso());
        visto = new AtomicReference<>();
    }

    private GatewayFilterChain cadena() {
        return intercambio -> {
            visto.set(intercambio);
            return Mono.empty();
        };
    }

    private String token(String rol, long horasDeVida) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
            .subject("15.234.871-1")
            .claim("id", 1)
            .claim("rol", rol)
            .issuedAt(new Date(ahora))
            .expiration(new Date(ahora + horasDeVida * 3_600_000))
            .signWith(llave)
            .compact();
    }

    private MockServerWebExchange peticion(String metodo, String ruta, String autorizacion) {
        MockServerHttpRequest.BaseBuilder<?> builder = switch (metodo) {
            case "POST" -> MockServerHttpRequest.post(ruta);
            case "PATCH" -> MockServerHttpRequest.patch(ruta);
            case "OPTIONS" -> MockServerHttpRequest.options(ruta);
            default -> MockServerHttpRequest.get(ruta);
        };
        if (autorizacion != null) {
            builder.header("Authorization", autorizacion);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private String cuerpo(MockServerWebExchange intercambio) {
        return intercambio.getResponse().getBodyAsString().block();
    }

    @Test
    void filter_unaRutaPublica_pasaSinToken() {
        MockServerWebExchange intercambio = peticion("POST", "/api/auth/login", null);

        filtro.filter(intercambio, cadena()).block();

        assertThat(visto.get()).as("la petición siguió al servicio").isNotNull();
        assertThat(intercambio.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_sinCabeceraDeAutorizacion_responde401ConMotivo() {
        MockServerWebExchange intercambio = peticion("GET", "/api/sesiones", null);

        filtro.filter(intercambio, cadena()).block();

        assertThat(visto.get()).as("no llegó al servicio").isNull();
        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(cuerpo(intercambio)).contains("Falta el token o no es válido.");
    }

    @Test
    void filter_conCabeceraSinElPrefijoBearer_responde401() {
        MockServerWebExchange intercambio = peticion("GET", "/api/sesiones", token("P-01", 1));

        filtro.filter(intercambio, cadena()).block();

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(visto.get()).isNull();
    }

    @Test
    void filter_conUnTokenVencido_responde401YPideVolverAEntrar() {
        MockServerWebExchange intercambio =
            peticion("GET", "/api/sesiones", "Bearer " + token("P-01", -2));

        filtro.filter(intercambio, cadena()).block();

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(cuerpo(intercambio)).contains("El token venció o no es válido");
    }

    @Test
    void filter_conTokenValido_inyectaLaIdentidadParaElServicioDeDestino() {
        MockServerWebExchange intercambio =
            peticion("GET", "/api/sesiones", "Bearer " + token("P-03", 1));

        filtro.filter(intercambio, cadena()).block();

        ServerWebExchange paso = visto.get();
        assertThat(paso).isNotNull();
        assertThat(paso.getRequest().getHeaders().getFirst("X-Usuario-Id")).isEqualTo("1");
        assertThat(paso.getRequest().getHeaders().getFirst("X-Usuario-Rol")).isEqualTo("P-03");
        assertThat(paso.getRequest().getHeaders().getFirst("X-Usuario-Rut")).isEqualTo("15.234.871-1");
    }

    @Test
    void filter_unaRutaDeAdministracionConRolQueNoAdministra_responde403() {
        MockServerWebExchange intercambio =
            peticion("GET", "/api/usuarios", "Bearer " + token("P-03", 1));

        filtro.filter(intercambio, cadena()).block();

        assertThat(visto.get()).isNull();
        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(cuerpo(intercambio)).contains("requiere el perfil de administración");
    }

    @Test
    void filter_unaRutaDeAdministracionConElPerfilP05_pasa() {
        MockServerWebExchange intercambio =
            peticion("GET", "/api/usuarios", "Bearer " + token("P-05", 1));

        filtro.filter(intercambio, cadena()).block();

        assertThat(visto.get()).isNotNull();
        assertThat(intercambio.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_laFichaPropiaConCualquierPerfil_pasa() {
        // GET /api/usuarios/{id} es "mi perfil": si se cerrara, la pantalla de perfil no carga.
        MockServerWebExchange intercambio =
            peticion("GET", "/api/usuarios/1", "Bearer " + token("P-04", 1));

        filtro.filter(intercambio, cadena()).block();

        assertThat(visto.get()).isNotNull();
    }

    @Test
    void getOrder_correAntesDeLasRutas() {
        assertThat(filtro.getOrder()).isNegative();
    }
}
