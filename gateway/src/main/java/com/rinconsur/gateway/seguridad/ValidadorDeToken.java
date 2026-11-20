package com.rinconsur.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Optional;

// Verifica la firma y la vigencia del token que emite el servicio de usuarios.
// Cualquier problema —firma, vencimiento, formato— se traduce a vacío: un token malo es un 401,
// nunca un 500.
@Component
public class ValidadorDeToken {

    private final SecretKey clave;

    public ValidadorDeToken(@Value("${rinconsur.seguridad.clave}") String claveBase64) {
        this.clave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(claveBase64));
    }

    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build()
                .parseSignedClaims(token).getPayload());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
