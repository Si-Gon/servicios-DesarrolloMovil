package com.rinconsur.gateway.excepcion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// El gateway es reactivo: los errores de ruteo no pasan por un controlador. Lo que se prueba acá
// es lo mismo que ve el navegador: 404 con motivo, 503 con el servicio abajo y 500 sin rastros.
@DisplayName("Errores del gateway")
class ManejadoresDeErrorTest {

    private final ManejadorDeErroresWeb web = new ManejadorDeErroresWeb();

    private MockServerWebExchange intercambio() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/contacto").build());
    }

    @Test
    void web_unaRutaQueNoExiste_responde404ConCuerpoPropio() {
        MockServerWebExchange ex = intercambio();

        web.handle(ex, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getResponse().getBodyAsString().block())
            .isEqualTo("{\"motivo\":\"La ruta solicitada no existe en la API.\"}");
    }

    @Test
    void web_unServicioCaido_responde503YNoUnStackTrace() {
        MockServerWebExchange ex = intercambio();

        web.handle(ex, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getResponse().getBodyAsString().block())
            .contains("Ocurrió un problema en la puerta de entrada.");
    }

    @Test
    void web_unErrorSinEstado_responde500() {
        MockServerWebExchange ex = intercambio();

        web.handle(ex, new IllegalStateException("boom")).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void web_un4xxQueNoEs404_conservaSuEstadoConMotivoGenerico() {
        MockServerWebExchange ex = intercambio();

        web.handle(ex, new ResponseStatusException(HttpStatus.BAD_REQUEST)).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponse().getBodyAsString().block())
            .contains("La petición no se pudo procesar.");
    }

    @Test
    void consejo_unIoExceptionDeNetty_esUn503Mostrable() {
        ResponseEntity<Map<String, String>> r = new ManejadorDeErrores().servicioAbajo(
            new IOException("Connection refused"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.getBody()).containsEntry("motivo", "El servicio no está disponible en este momento.");
    }

    @Test
    void consejo_unTimeout_tambienEs503() {
        ResponseEntity<Map<String, String>> r = new ManejadorDeErrores().servicioAbajo(
            new java.util.concurrent.TimeoutException("no respondió"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
