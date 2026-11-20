package com.rinconsur.usuarios.seguridad;

import com.rinconsur.usuarios.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

// El token lleva el rol dentro: el gateway lo valida y los servicios de negocio leen el rol
// sin tener que preguntarle nada a este servicio.
@Service
public class JwtServicio {

    private final SecretKey clave;
    private final long horas;

    public JwtServicio(@Value("${rinconsur.seguridad.clave}") String claveBase64,
                       @Value("${rinconsur.seguridad.horas-token:8}") long horas) {
        this.clave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(claveBase64));
        this.horas = horas;
    }

    public String emitir(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
            .subject(u.getRut())
            .claim("id", u.getId())
            .claim("rol", u.getRol().getCodigo())
            .claim("nombre", u.nombreCompleto())
            .claim("cargo", u.getCargo())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(horas, ChronoUnit.HOURS)))
            .signWith(clave)
            .compact();
    }
}
