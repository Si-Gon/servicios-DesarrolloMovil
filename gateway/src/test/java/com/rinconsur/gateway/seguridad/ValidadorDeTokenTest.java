package com.rinconsur.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Un token malo tiene que ser un 401, nunca un 500 ni un 200: por eso el validador traduce
// cualquier problema a vacío en vez de dejar escapar la excepción de la librería.
@DisplayName("Validador de token del gateway")
class ValidadorDeTokenTest {

    // 32 bytes en base64: es el largo mínimo que exige HS256.
    private static final String CLAVE = Base64.getEncoder()
        .encodeToString("clave-de-prueba-de-32-bytes-abcdef".getBytes());

    private final ValidadorDeToken validador = new ValidadorDeToken(CLAVE);

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(CLAVE));
    }

    private String token(Date emision, Date vence, String rol) {
        return Jwts.builder()
            .subject("15.234.871-1")
            .claim("id", 1)
            .claim("rol", rol)
            .issuedAt(emision)
            .expiration(vence)
            .signWith(clave())
            .compact();
    }

    @Test
    void validar_unTokenVigente_devuelveElPerfilYElRutDeDentro() {
        String valido = token(new Date(), new Date(System.currentTimeMillis() + 3_600_000), "P-01");

        Optional<Claims> claims = validador.validar(valido);

        assertThat(claims).isPresent();
        assertThat(claims.get().get("rol")).isEqualTo("P-01");
        assertThat(claims.get().getSubject()).isEqualTo("15.234.871-1");
        assertThat(claims.get().get("id")).isEqualTo(1);
    }

    @Test
    void validar_unTokenVencido_devuelveVacio() {
        String vencido = token(new Date(System.currentTimeMillis() - 7_200_000),
            new Date(System.currentTimeMillis() - 3_600_000), "P-01");

        assertThat(validador.validar(vencido)).isEmpty();
    }

    @Test
    void validar_unTokenConLaFirmaCambiada_devuelveVacio() {
        String valido = token(new Date(), new Date(System.currentTimeMillis() + 3_600_000), "P-01");
        // Se cambia el último carácter de la firma: el contenido es el mismo, la firma no.
        String manipulado = valido.substring(0, valido.length() - 2)
            + (valido.endsWith("A") ? "B" : "A");

        assertThat(validador.validar(manipulado)).isEmpty();
    }

    @Test
    void validar_unTokenFirmadoConOtraClave_devuelveVacio() {
        SecretKey otra = Keys.hmacShaKeyFor("otra-clave-de-prueba-de-32-bytes-xyz".getBytes());
        String ajeno = Jwts.builder().subject("1").signWith(otra).compact();

        assertThat(validador.validar(ajeno)).isEmpty();
    }

    @Test
    void validar_unTextoQueNoEsUnToken_devuelveVacioSinReventar() {
        assertThat(validador.validar("no-soy-un-token")).isEmpty();
        assertThat(validador.validar("")).isEmpty();
    }
}
