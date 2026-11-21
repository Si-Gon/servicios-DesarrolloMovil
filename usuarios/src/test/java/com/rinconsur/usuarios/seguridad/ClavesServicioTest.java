package com.rinconsur.usuarios.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Conviven dos formatos a propósito: las cuentas sembradas guardan sha256("RUT|clave") y las
// nuevas guardan PBKDF2 con su sal. Si el servicio dejara de aceptar el primero, nadie sembrado
// podría entrar y el problema aparecería recién en la demo.
@DisplayName("Claves y códigos temporales")
class ClavesServicioTest {

    private final ClavesServicio claves = new ClavesServicio();

    @Test
    void hashear_laMismaClaveDosVeces_daHashesDistintosPorLaSal() {
        String a = claves.hashear("15.234.871-1", "rincon2026");
        String b = claves.hashear("15.234.871-1", "rincon2026");

        assertThat(a).startsWith("pbkdf2$").isNotEqualTo(b);
    }

    @Test
    void verificar_unaClaveCorrectaContraSuHash_devuelveTrue() {
        String hash = claves.hashear("15.234.871-1", "rincon2026");

        assertThat(claves.verificar("15.234.871-1", "rincon2026", hash)).isTrue();
        assertThat(claves.verificar("15.234.871-1", "otraClave", hash)).isFalse();
    }

    @Test
    void verificar_unHashLegadoSha256_aceptaLaClaveDeLaSemilla() {
        // Es el formato que escribe la app Android: sha256("RUT|clave") en hexadecimal.
        String legado = "5d0e7c0e5a6f42a53b9f0f1d6c0f4a3e9c1d2b3a4c5d6e7f8091a2b3c4d5e6f7";
        String calculado = claves.hashLegado("15.234.871-1", "rincon2026");

        assertThat(claves.verificar("15.234.871-1", "rincon2026", calculado)).isTrue();
        assertThat(claves.verificar("15.234.871-1", "rincon2026", calculado.toUpperCase())).isTrue();
        assertThat(claves.verificar("15.234.871-1", "rincon2026", legado)).isFalse();
    }

    @Test
    void verificar_unHashRotoONulo_noRevientaYDevuelveFalse() {
        assertThat(claves.verificar("1-9", "x", null)).isFalse();
        assertThat(claves.verificar("1-9", null, "pbkdf2$1$a$b")).isFalse();
        assertThat(claves.verificar("1-9", "x", "pbkdf2$mal-formado")).isFalse();
    }

    @Test
    void codigoTemporal_ochoCaracteresSinLetrasQueSeConfundenAlDictar() {
        String codigo = claves.codigoTemporal();

        assertThat(codigo).hasSize(8).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        assertThat(codigo).doesNotContain("O").doesNotContain("I").doesNotContain("0").doesNotContain("1");
    }
}
