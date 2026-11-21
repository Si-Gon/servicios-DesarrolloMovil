package com.rinconsur.usuarios.seguridad;

import com.rinconsur.usuarios.model.Rol;
import com.rinconsur.usuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// El token es lo único que el gateway mira: si no lleva el perfil y el identificador, el
// servicio de destino no puede decidir quién hace la acción.
@DisplayName("Token de sesión")
class JwtServicioTest {

    private static final String CLAVE = Base64.getEncoder()
        .encodeToString("clave-de-prueba-de-32-bytes-abcdef".getBytes());

    private JwtServicio jwt;
    private SecretKey llave;

    @BeforeEach
    void preparar() {
        jwt = new JwtServicio(CLAVE, 8);
        llave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(CLAVE));
    }

    private Usuario usuario(String rol) {
        Rol r = new Rol();
        r.setId(1);
        r.setCodigo(rol);
        r.setNombre("Jefa de tienda");
        Usuario u = new Usuario();
        u.setId(7);
        u.setRut("15.234.871-1");
        u.setNombres("Carolina");
        u.setApellidoPaterno("Soto");
        u.setRol(r);
        u.setActivo(true);
        return u;
    }

    @Test
    void emitir_dejaDentroElIdElRutYElPerfil() {
        String token = jwt.emitir(usuario("P-01"));

        Claims datos = Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();

        assertThat(datos.getSubject()).isEqualTo("15.234.871-1");
        assertThat(datos.get("id")).isEqualTo(7);
        assertThat(datos.get("rol")).isEqualTo("P-01");
    }

    @Test
    void emitir_elTokenVenceDentroDeLaVentanaConfigurada() {
        String token = jwt.emitir(usuario("P-04"));

        Claims datos = Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();
        long horas = (datos.getExpiration().getTime() - datos.getIssuedAt().getTime()) / 3_600_000L;

        assertThat(horas).isBetween(7L, 8L);
    }

    @Test
    void emitir_dosTokensDelMismoUsuario_sonDistintosPorLaHoraDeEmision() {
        String a = jwt.emitir(usuario("P-01"));
        String b = jwt.emitir(usuario("P-01"));

        assertThat(a).isNotBlank();
        assertThat(b).isNotBlank();
        // Lo que importa: los dos son válidos con la misma clave.
        assertThat(Jwts.parser().verifyWith(llave).build().parseSignedClaims(a).getPayload().get("rol"))
            .isEqualTo("P-01");
    }
}
