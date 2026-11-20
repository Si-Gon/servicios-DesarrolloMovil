package com.rinconsur.usuarios.seguridad;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Dos formatos conviven a propósito: las cuentas sembradas guardan sha256("RUT|clave"), que es
// lo que usa la app desde la EA2, y las cuentas nuevas guardan PBKDF2 con su sal. El servicio
// acepta los dos y no obliga a nadie a cambiar la clave para poder entrar.
@Service
public class ClavesServicio {

    private static final String PREFIJO = "pbkdf2$";
    private static final int ITERACIONES = 120_000;
    private static final int LARGO = 256;
    private final SecureRandom azar = new SecureRandom();

    public String hashear(String rut, String clave) {
        byte[] sal = new byte[16];
        azar.nextBytes(sal);
        byte[] derivada = derivar(clave.toCharArray(), sal, ITERACIONES);
        return PREFIJO + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$"
            + Base64.getEncoder().encodeToString(derivada);
    }

    public boolean verificar(String rut, String clave, String hashGuardado) {
        if (hashGuardado == null || clave == null) {
            return false;
        }
        if (!hashGuardado.startsWith(PREFIJO)) {
            return hashLegado(rut, clave).equalsIgnoreCase(hashGuardado);
        }
        String[] partes = hashGuardado.split("\\$");
        if (partes.length != 4) {
            return false;
        }
        int iteraciones = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperada = Base64.getDecoder().decode(partes[3]);
        byte[] calculada = derivar(clave.toCharArray(), sal, iteraciones);
        return MessageDigest.isEqual(esperada, calculada);
    }

    public String hashLegado(String rut, String clave) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] salida = sha.digest((rut + "|" + clave).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave.", e);
        }
    }

    // Código temporal legible: sin letras que se confundan al dictarlo (0/O, 1/I).
    public String codigoTemporal() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(alfabeto.charAt(azar.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }

    private byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave, sal, iteraciones, LARGO);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la clave.", e);
        }
    }
}
