package com.rinconsur.usuarios.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// El módulo 11 es lo que impide que un RUT inventado entre al sistema o que la misma persona
// quede dos veces con el dígito cambiado.
@DisplayName("RUT chileno")
class RutServicioTest {

    private final RutServicio ruts = new RutServicio();

    @Test
    void esValido_unRutConSuDigitoCorrecto_esAceptadoConPuntosYSinPuntos() {
        assertThat(ruts.esValido("15.234.871-1")).isTrue();
        assertThat(ruts.esValido("15234871-1")).isTrue();
        assertThat(ruts.esValido("19.876.345-4")).isTrue();
    }

    @Test
    void esValido_unRutConDigitoK_seAceptaEnMayuscula() {
        // 10.000.013 es el cuerpo más bajo cuyo dígito verificador es K.
        assertThat(ruts.esValido("10.000.013-K")).isTrue();
        assertThat(ruts.esValido("10.000.013-k")).isTrue();
    }

    @Test
    void esValido_unDigitoVerificadorEquivocado_seRechaza() {
        assertThat(ruts.esValido("15.234.871-2")).isFalse();
        assertThat(ruts.esValido("19.876.345-9")).isFalse();
    }

    @Test
    void esValido_unTextoQueNoTieneFormaDeRut_seRechaza() {
        assertThat(ruts.esValido(null)).isFalse();
        assertThat(ruts.esValido("")).isFalse();
        assertThat(ruts.esValido("1234567")).as("sin guion no es un RUT").isFalse();
        assertThat(ruts.esValido("12-3")).isFalse();
        assertThat(ruts.esValido("ABCDEFG-1")).isFalse();
    }

    @Test
    void normalizar_unRutSinPuntos_loDejaComoSeMuestraEnLaApp() {
        assertThat(ruts.normalizar("15234871-1")).isEqualTo("15.234.871-1");
    }

    @Test
    void normalizar_unRutConEspaciosYMinuscula_loDejaCanonico() {
        assertThat(ruts.normalizar(" 10000013-k ")).isEqualTo("10.000.013-K");
    }
}
