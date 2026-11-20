package com.rinconsur.usuarios.seguridad;

import org.springframework.stereotype.Service;

// Módulo 11: el mismo cálculo que hace la app, para que un RUT inventado no entre al sistema
// y para no guardar dos veces la misma persona con el dígito cambiado.
@Service
public class RutServicio {

    public boolean esValido(String rut) {
        if (rut == null) {
            return false;
        }
        String limpio = rut.replace(".", "").replace(" ", "").toUpperCase();
        if (!limpio.matches("\\d{7,8}-[\\dK]")) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.indexOf('-'));
        char dv = limpio.charAt(limpio.length() - 1);
        return digitoVerificador(cuerpo) == dv;
    }

    public String normalizar(String rut) {
        String limpio = rut.replace(".", "").replace(" ", "").toUpperCase();
        String cuerpo = limpio.substring(0, limpio.indexOf('-'));
        char dv = limpio.charAt(limpio.length() - 1);
        // Formato con puntos, que es como se ve en la app.
        String conPuntos = String.format("%,d", Long.parseLong(cuerpo)).replace(",", ".");
        return conPuntos + "-" + dv;
    }

    private char digitoVerificador(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return Character.forDigit(resto, 10);
    }
}
