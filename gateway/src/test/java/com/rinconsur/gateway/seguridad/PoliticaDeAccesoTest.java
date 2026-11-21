package com.rinconsur.gateway.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// La política de acceso es la tabla que se defiende en la presentación: qué se pide sin token y
// qué exige el perfil de administración. Un error acá abre la puerta o la cierra para quien no
// corresponde, y las dos cosas se notan recién en la demo.
@DisplayName("Política de acceso del gateway")
class PoliticaDeAccesoTest {

    private final PoliticaDeAcceso politica = new PoliticaDeAcceso();

    @Test
    void esPublica_elLoginYLasPreguntas_quedanAbiertasSinToken() {
        assertThat(politica.esPublica("POST", "/api/auth/login")).isTrue();
        assertThat(politica.esPublica("GET", "/api/auth/preguntas")).isTrue();
    }

    @Test
    void esPublica_laDocumentacion_quedaAbiertaParaRevisarLaApiSinCredenciales() {
        assertThat(politica.esPublica("GET", "/v3/api-docs/catalogos")).isTrue();
        assertThat(politica.esPublica("GET", "/swagger-ui/index.html")).isTrue();
        assertThat(politica.esPublica("GET", "/docs/medicion/v3/api-docs")).isTrue();
    }

    @Test
    void esPublica_elPreflightDelNavegador_noExigeToken() {
        // Sin esto el navegador no puede ni preguntar si la petición está permitida.
        assertThat(politica.esPublica("OPTIONS", "/api/sesiones")).isTrue();
    }

    @Test
    void esPublica_unaRutaDeNegocio_noLoEs() {
        assertThat(politica.esPublica("GET", "/api/sesiones")).isFalse();
        assertThat(politica.esPublica("POST", "/api/registros")).isFalse();
        assertThat(politica.esPublica("GET", "/api/indicadores/2026-09-15")).isFalse();
    }

    @Test
    void esSoloAdministrador_laListaDeCuentas_esDelAdministradorAunqueSeaUnGet() {
        assertThat(politica.esSoloAdministrador("GET", "/api/usuarios")).isTrue();
    }

    @Test
    void esSoloAdministrador_laFichaDeUnaPersona_laPuedePedirCualquieraConSesion() {
        // GET /api/usuarios/{id} es "mi perfil": cerrarlo rompe la pantalla de perfil.
        assertThat(politica.esSoloAdministrador("GET", "/api/usuarios/5")).isFalse();
    }

    @Test
    void esSoloAdministrador_escribirSobreUnaCuenta_exigeAdministracion() {
        assertThat(politica.esSoloAdministrador("PATCH", "/api/usuarios/5")).isTrue();
        assertThat(politica.esSoloAdministrador("DELETE", "/api/usuarios/5")).isTrue();
    }

    @Test
    void esAdministrador_soloElPerfilP05_loEs() {
        assertThat(politica.esAdministrador("P-05")).isTrue();
        assertThat(politica.esAdministrador("P-01")).isFalse();
        assertThat(politica.esAdministrador("P-04")).isFalse();
        assertThat(politica.esAdministrador(null)).isFalse();
    }

    @Test
    void esSoloAdministrador_lasRutasDeMedicion_noSonSoloDeAdministracion() {
        assertThat(politica.esSoloAdministrador("POST", "/api/sesiones")).isFalse();
        assertThat(politica.esSoloAdministrador("POST", "/api/registros")).isFalse();
    }
}
