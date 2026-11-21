package com.rinconsur.catalogos.excepcion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// El contrato de error del servicio: siempre {"motivo": "..."} con tildes, y el estado que
// corresponde. Sin esto, un 500 sale con el cuerpo por defecto de Spring y la app no puede
// explicar nada al usuario.
@DisplayName("Errores uniformes de catálogos")
class ManejadorDeErroresTest {

    private final ManejadorDeErrores manejador = new ManejadorDeErrores();

    @Test
    void cadaFabricaDeErrorDeNegocio_traeSuEstado() {
        assertThat(ErrorDeNegocio.invalido("x").getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorDeNegocio.noAutorizado("x").getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorDeNegocio.prohibido("x").getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ErrorDeNegocio.noEncontrado("x").getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorDeNegocio.conflicto("x").getEstado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorDeNegocio.conflicto("x").getMotivo()).isEqualTo("x");
    }

    @Test
    void deNegocio_respetaElEstadoYElTextoQueDecidioElServicio() {
        ResponseEntity<Map<String, String>> r =
            manejador.deNegocio(ErrorDeNegocio.noEncontrado("No existe un acceso con el código A9."));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("motivo", "No existe un acceso con el código A9.");
    }

    @Test
    void deParametro_unParametroConElTipoEquivocado_esUn400() {
        ResponseEntity<Map<String, String>> r = manejador.deParametro(
            new MissingServletRequestParameterException("fecha", "LocalDate"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("motivo", "Alguno de los datos enviados no es válido.");
    }

    @Test
    void deParametro_unTipoQueNoCalza_esElMismo400() {
        ResponseEntity<Map<String, String>> r = manejador.deParametro(
            new MethodArgumentTypeMismatchException("no-es-fecha", java.time.LocalDate.class, "fecha", null, null));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deBase_unChoqueDeUnicidad_es409YNoUn500() {
        ResponseEntity<Map<String, String>> r = manejador.deBase(
            new DataIntegrityViolationException("Duplicate entry 'A1' for key 'codigo'"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).containsEntry("motivo",
            "Ya existe un registro con ese código en los datos maestros.");
    }

    @Test
    void deRuta_unaRutaInexistente_es404ConMotivo() {
        ResponseEntity<Map<String, String>> r = manejador.deRuta(
            new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/catalogos/nada", null));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("motivo", "La ruta solicitada no existe en este servicio.");
    }

    @Test
    void deValidacion_devuelveElPrimerMensajeDelDtoQueYaEstaEnCastellano() {
        org.springframework.validation.BeanPropertyBindingResult resultado =
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "peticion");
        resultado.addError(new org.springframework.validation.FieldError(
            "peticion", "rut", "El RUT es obligatorio."));
        org.springframework.web.bind.MethodArgumentNotValidException e =
            new org.springframework.web.bind.MethodArgumentNotValidException(null, resultado);

        ResponseEntity<Map<String, String>> r = manejador.deValidacion(e);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("motivo", "El RUT es obligatorio.");
    }

    @Test
    void deValidacion_sinCampoMarcado_respondeConUnTextoGenerico() {
        org.springframework.validation.BeanPropertyBindingResult resultado =
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "peticion");
        org.springframework.web.bind.MethodArgumentNotValidException e =
            new org.springframework.web.bind.MethodArgumentNotValidException(null, resultado);

        ResponseEntity<Map<String, String>> r = manejador.deValidacion(e);

        assertThat(r.getBody()).containsEntry("motivo", "Los datos enviados no son válidos.");
    }

    @Test
    void deCuerpoIlegible_unCuerpoQueNoEsJson_esUn400QueLoExplica() {
        org.springframework.http.converter.HttpMessageNotReadableException e =
            new org.springframework.http.converter.HttpMessageNotReadableException(
                "JSON roto", new org.springframework.mock.http.MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> r = manejador.deCuerpoIlegible(e);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("motivo", "El cuerpo de la petición no es válido.");
    }

    @Test
    void generico_loInesperado_es500YNoCuentaLaBase() {
        ResponseEntity<Map<String, String>> r =
            manejador.generico(new IllegalStateException("Connection pool exhausted"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody()).containsEntry("motivo", "Ocurrió un problema en el servicio de catálogos.");
        assertThat(r.getBody().get("motivo")).doesNotContain("Connection pool");
    }
}
