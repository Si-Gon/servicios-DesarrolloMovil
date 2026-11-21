package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.dto.RegistroVista;
import com.rinconsur.medicion.dto.SesionVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.excepcion.ManejadorDeErrores;
import com.rinconsur.medicion.model.EstadoSesion;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.registro.RegistroServicio;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import com.rinconsur.medicion.sesion.SesionServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El contrato que consume la app: el código de estado y el texto del motivo. Se prueba el
// controlador solo, con los servicios simulados, pero con el consejo de excepciones real.
@DisplayName("API de sesiones y registros")
class SesionesYRegistrosControllerTest {

    private SesionRepositorio sesiones;
    private RegistroRepositorio registros;
    private SesionServicio servicioSesiones;
    private RegistroServicio servicioRegistros;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        sesiones = mock(SesionRepositorio.class);
        registros = mock(RegistroRepositorio.class);
        servicioSesiones = mock(SesionServicio.class);
        servicioRegistros = mock(RegistroServicio.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new SesionesController(sesiones, registros, servicioSesiones),
                new RegistrosController(servicioRegistros))
            .setControllerAdvice(new ManejadorDeErrores())
            .build();
    }

    private SesionObservacion sesion() {
        EstadoSesion e = new EstadoSesion();
        e.setId(3);
        e.setNombre("Finalizada");
        SesionObservacion s = new SesionObservacion();
        s.setId(1);
        s.setCodigo("S-0001");
        s.setIdAcceso(4);
        s.setIdBloque(4);
        s.setFecha(LocalDate.of(2026, 9, 15));
        s.setHoraInicioProg(LocalTime.of(10, 0));
        s.setHoraFinProg(LocalTime.of(11, 0));
        s.setIdObservador(5);
        s.setObservadorNombre("Tamara Rojas");
        s.setEstado(e);
        return s;
    }

    @Test
    void deFecha_sinParametro_usaElDiaDeHoyYDevuelveLasSesiones() throws Exception {
        when(sesiones.findByFechaOrderByHoraInicioProgAsc(any())).thenReturn(List.of(sesion()));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(30L);

        mvc.perform(get("/api/sesiones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigo").value("S-0001"))
            .andExpect(jsonPath("$[0].registros").value(30))
            .andExpect(jsonPath("$[0].estado").value("Finalizada"));
    }

    @Test
    void porValidar_devuelveLasCerradasQueEsperanRevision() throws Exception {
        when(sesiones.findByEstadoIdOrderByFechaAscHoraInicioProgAsc(3)).thenReturn(List.of(sesion()));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(31L);

        mvc.perform(get("/api/sesiones/por-validar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].observador").value("Tamara Rojas"));
    }

    @Test
    void porId_unaSesionQueNoExiste_responde404ConMotivo() throws Exception {
        when(servicioSesiones.buscar(99))
            .thenThrow(ErrorDeNegocio.noEncontrado("No existe una sesión con ese identificador."));

        mvc.perform(get("/api/sesiones/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.motivo").value("No existe una sesión con ese identificador."));
    }

    @Test
    void programar_pasaElProgramadorDelTokenYDevuelve201() throws Exception {
        when(servicioSesiones.programar(any(), eq(3))).thenReturn(
            new SesionVista(1, "S-0008", 4, 4, LocalDate.of(2026, 9, 16),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Programada", "Tamara Rojas", 0));

        mvc.perform(post("/api/sesiones")
                .header("X-Usuario-Id", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idAcceso":4,"fecha":"2026-09-16","horaInicio":"10:00","horaFin":"11:00",
                     "idObservador":5,"observadorNombre":"Tamara Rojas"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("S-0008"));
    }

    @Test
    void programar_sinElAcceso_responde400ConElMensajeDelDto() throws Exception {
        mvc.perform(post("/api/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fecha":"2026-09-16","horaInicio":"10:00","horaFin":"11:00","idObservador":5}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value("Indica el acceso de la sesión."));
    }

    @Test
    void programar_conLaHoraInvertida_responde400ConElMotivoDelServicio() throws Exception {
        when(servicioSesiones.programar(any(), any()))
            .thenThrow(ErrorDeNegocio.invalido("La hora de término tiene que ser posterior a la de inicio."));

        mvc.perform(post("/api/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idAcceso":4,"fecha":"2026-09-16","horaInicio":"11:00","horaFin":"10:00",
                     "idObservador":5}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value("La hora de término tiene que ser posterior a la de inicio."));
    }

    @Test
    void iniciar_sinCuerpo_usaLaHoraActualYDevuelveLaSesion() throws Exception {
        when(servicioSesiones.iniciar(eq(1), eq(5), any())).thenReturn(
            new SesionVista(1, "S-0001", 4, 4, LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "En curso", "Tamara Rojas", 0));

        mvc.perform(patch("/api/sesiones/1/iniciar").header("X-Usuario-Id", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("En curso"));
    }

    @Test
    void cerrar_conPocosRegistros_responde400DiciendoCuantosLleva() throws Exception {
        when(servicioSesiones.cerrar(eq(1), any(), any()))
            .thenThrow(ErrorDeNegocio.invalido("La sesión tiene 12 registros y el mínimo es 30. Sigue midiendo o anula la sesión."));

        mvc.perform(patch("/api/sesiones/1/cerrar").header("X-Usuario-Id", "5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value(
                "La sesión tiene 12 registros y el mínimo es 30. Sigue midiendo o anula la sesión."));
    }

    @Test
    void validar_sinSerElObservador_devuelve403() throws Exception {
        when(servicioSesiones.validar(1, 5))
            .thenThrow(ErrorDeNegocio.prohibido("No puedes validar una sesión que observaste tú."));

        mvc.perform(patch("/api/sesiones/1/validar").header("X-Usuario-Id", "5"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.motivo").value("No puedes validar una sesión que observaste tú."));
    }

    @Test
    void validar_unaSesionEnCurso_responde409ConElEstadoReal() throws Exception {
        when(servicioSesiones.validar(1, 3))
            .thenThrow(ErrorDeNegocio.conflicto(
                "Solo se valida una sesión cerrada por el observador. Ahora está en «En curso»."));

        mvc.perform(patch("/api/sesiones/1/validar").header("X-Usuario-Id", "3"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.motivo").value(
                "Solo se valida una sesión cerrada por el observador. Ahora está en «En curso»."));
    }

    @Test
    void rechazar_armaElMotivoConElDetalle() throws Exception {
        mvc.perform(patch("/api/sesiones/1/rechazar")
                .header("X-Usuario-Id", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"No cuadra el conteo\",\"detalle\":\"Faltan dos bloques\"}"))
            .andExpect(status().isOk());

        verify(servicioSesiones).rechazar(1, 3, "No cuadra el conteo: Faltan dos bloques");
    }

    @Test
    void rechazar_sinDetalle_dejaSoloElMotivo() throws Exception {
        mvc.perform(patch("/api/sesiones/1/rechazar")
                .header("X-Usuario-Id", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"No cuadra el conteo\"}"))
            .andExpect(status().isOk());

        verify(servicioSesiones).rechazar(1, 3, "No cuadra el conteo");
    }

    @Test
    void registrar_devuelve201ConElRegistroGuardado() throws Exception {
        when(servicioRegistros.registrar(any(), eq(5))).thenReturn(
            new RegistroVista(99, 1, java.time.LocalDateTime.now(), 1, 4, 2, false, 2, false));

        mvc.perform(post("/api/registros")
                .header("X-Usuario-Id", "5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSesion\":1,\"idClasificacion\":1,\"idRango\":4,\"idTipoCompania\":2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    void registrar_enUnaSesionCerrada_responde409() throws Exception {
        when(servicioRegistros.registrar(any(), any()))
            .thenThrow(ErrorDeNegocio.conflicto("La sesión no está en curso: ahora figura como «Finalizada»."));

        mvc.perform(post("/api/registros")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSesion\":1,\"idClasificacion\":1,\"idRango\":4,\"idTipoCompania\":2}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.motivo").value(
                "La sesión no está en curso: ahora figura como «Finalizada»."));
    }

    @Test
    void anular_armaElMotivoConElDetalle() throws Exception {
        when(servicioRegistros.anular(eq(7), eq(3), any())).thenReturn(
            new RegistroVista(7, 1, java.time.LocalDateTime.now(), 1, 4, 2, false, null, true));

        mvc.perform(patch("/api/registros/7/anular")
                .header("X-Usuario-Id", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Duplicado\",\"detalle\":\"Misma persona dos veces\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anulado").value(true));

        verify(servicioRegistros).anular(7, 3, "Duplicado: Misma persona dos veces");
    }

    @Test
    void ultimos_devuelveLosDiezDeLaSesion() throws Exception {
        when(servicioRegistros.ultimos(1)).thenReturn(List.of(
            new RegistroVista(99, 1, java.time.LocalDateTime.now(), 2, 4, 1, true, 3, false)));

        mvc.perform(get("/api/registros/sesion/1/ultimos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].idClasificacion").value(2));
    }
}
