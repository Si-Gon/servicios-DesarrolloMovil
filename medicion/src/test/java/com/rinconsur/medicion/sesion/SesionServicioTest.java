package com.rinconsur.medicion.sesion;

import com.rinconsur.medicion.catalogo.CatalogoServicio;
import com.rinconsur.medicion.dto.ProgramarSesionPeticion;
import com.rinconsur.medicion.dto.SesionVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.model.EstadoSesion;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.repository.EstadoSesionRepositorio;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// La máquina de estados de la sesión: programada → en curso → finalizada → validada o rechazada.
// Cada paso exige el estado anterior y responde con el estado real, porque la pantalla pudo
// haberse abierto hace diez minutos y el estado cambió mientras tanto.
@DisplayName("Reglas de la sesión de observación")
class SesionServicioTest {

    private SesionRepositorio sesiones;
    private RegistroRepositorio registros;
    private EstadoSesionRepositorio estados;
    private CatalogoServicio catalogo;
    private SesionServicio servicio;

    @BeforeEach
    void preparar() {
        sesiones = mock(SesionRepositorio.class);
        registros = mock(RegistroRepositorio.class);
        estados = mock(EstadoSesionRepositorio.class);
        catalogo = mock(CatalogoServicio.class);
        servicio = new SesionServicio(sesiones, registros, estados, catalogo);

        when(estados.findByNombre("Programada")).thenReturn(Optional.of(estado(1, "Programada")));
        when(estados.findByNombre("En curso")).thenReturn(Optional.of(estado(2, "En curso")));
        when(estados.findByNombre("Finalizada")).thenReturn(Optional.of(estado(3, "Finalizada")));
        when(estados.findByNombre("Validada")).thenReturn(Optional.of(estado(4, "Validada")));
        when(estados.findByNombre("Rechazada")).thenReturn(Optional.of(estado(5, "Rechazada")));
        when(sesiones.save(any(SesionObservacion.class))).thenAnswer(i -> i.getArgument(0));
        when(sesiones.count()).thenReturn(7L);
        when(sesiones.existsByCodigo(anyString())).thenReturn(false);
    }

    private EstadoSesion estado(int id, String nombre) {
        EstadoSesion e = new EstadoSesion();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

    private SesionObservacion sesion(int id, String nombreEstado, int idObservador) {
        SesionObservacion s = new SesionObservacion();
        s.setId(id);
        s.setCodigo("S-00" + id);
        s.setIdAcceso(4);
        s.setFecha(LocalDate.of(2026, 9, 15));
        s.setHoraInicioProg(LocalTime.of(10, 0));
        s.setHoraFinProg(LocalTime.of(11, 0));
        s.setIdBloque(4);
        s.setIdObservador(idObservador);
        s.setIdProgramador(3);
        s.setObservadorNombre("Tamara Rojas");
        s.setEstado(estado(1, nombreEstado));
        return s;
    }

    private ProgramarSesionPeticion peticion() {
        return new ProgramarSesionPeticion(4, LocalDate.of(2026, 9, 16),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 5, "Tamara Rojas");
    }

    @Test
    void programar_unaSesionValida_guardaElBloqueQueDeduceElCatalogo() {
        when(catalogo.bloqueDe(LocalTime.of(10, 0))).thenReturn(4);

        SesionVista vista = servicio.programar(peticion(), 3);

        assertThat(vista.idAcceso()).isEqualTo(4);
        assertThat(vista.idBloque()).as("el bloque no se escribe a mano").isEqualTo(4);
        assertThat(vista.estado()).isEqualTo("Programada");
        assertThat(vista.observador()).isEqualTo("Tamara Rojas");
        assertThat(vista.codigo()).isEqualTo("S-0008");
    }

    @Test
    void programar_conLaHoraDeTerminoAntesDeLaDeInicio_loRechaza() {
        ProgramarSesionPeticion mala = new ProgramarSesionPeticion(4, LocalDate.of(2026, 9, 16),
            LocalTime.of(11, 0), LocalTime.of(10, 0), 5, "Tamara Rojas");

        assertThatThrownBy(() -> servicio.programar(mala, 3))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("posterior a la de inicio");

        verify(sesiones, never()).save(any(SesionObservacion.class));
    }

    @Test
    void programar_sinSaberQuienPrograma_loRechaza() {
        assertThatThrownBy(() -> servicio.programar(peticion(), null))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Falta la identidad de quien programa");
    }

    @Test
    void programar_conUnAccesoQueNoExiste_loDiceElCatalogoYNoSeGuarda() {
        org.mockito.Mockito.doThrow(ErrorDeNegocio.invalido("El acceso 9 no existe en los catálogos."))
            .when(catalogo).exigirAcceso(9);
        ProgramarSesionPeticion mala = new ProgramarSesionPeticion(9, LocalDate.of(2026, 9, 16),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 5, "Tamara Rojas");

        assertThatThrownBy(() -> servicio.programar(mala, 3))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("no existe en los catálogos");

        verify(sesiones, never()).save(any(SesionObservacion.class));
    }

    @Test
    void programar_conUnCodigoYaTomado_buscaElSiguienteLibre() {
        when(sesiones.existsByCodigo("S-0008")).thenReturn(true);
        when(sesiones.existsByCodigo("S-0009")).thenReturn(false);
        when(catalogo.bloqueDe(any())).thenReturn(4);

        SesionVista vista = servicio.programar(peticion(), 3);

        assertThat(vista.codigo()).isEqualTo("S-0009");
    }

    @Test
    void iniciar_unaSesionProgramada_laDejaEnCursoConLaHoraReal() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Programada", 5)));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(0L);

        SesionVista vista = servicio.iniciar(1, 5, LocalTime.of(10, 7));

        assertThat(vista.estado()).isEqualTo("En curso");
        ArgumentCaptor<SesionObservacion> guardada = ArgumentCaptor.forClass(SesionObservacion.class);
        verify(sesiones).save(guardada.capture());
        assertThat(guardada.getValue().getInicioReal()).isNotNull();
        assertThat(guardada.getValue().getInicioReal().toLocalTime()).isEqualTo(LocalTime.of(10, 7));
    }

    @Test
    void iniciar_unaSesionQueYaEstaEnCurso_avisaConElEstadoReal() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "En curso", 5)));

        assertThatThrownBy(() -> servicio.iniciar(1, 5, LocalTime.of(10, 7)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Solo se inicia una sesión programada.")
            .hasMessageContaining("En curso");
    }

    @Test
    void iniciar_laSesionDeOtraPersona_esProhibido() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Programada", 5)));

        assertThatThrownBy(() -> servicio.iniciar(1, 9, LocalTime.of(10, 7)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("asignada a otra persona");

        verify(sesiones, never()).save(any(SesionObservacion.class));
    }

    @Test
    void iniciar_unaSesionQueNoExiste_da404() {
        when(sesiones.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar(99, 5, LocalTime.of(10, 7)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No existe una sesión");
    }

    @Test
    void cerrar_conMenosRegistrosQueElMinimo_diceCuantosLlevaYNoCierra() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "En curso", 5)));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(12L);

        assertThatThrownBy(() -> servicio.cerrar(1, 5, LocalTime.of(11, 0)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("La sesión tiene 12 registros y el mínimo es 30.");

        verify(sesiones, never()).save(any(SesionObservacion.class));
    }

    @Test
    void cerrar_conLosTreintaRegistrosDelCaso_cierraLaSesion() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "En curso", 5)));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(32L);

        SesionVista vista = servicio.cerrar(1, 5, LocalTime.of(11, 2));

        assertThat(vista.estado()).isEqualTo("Finalizada");
        assertThat(vista.registros()).isEqualTo(32);
    }

    @Test
    void cerrar_unaSesionQueSoloEstaProgramada_noLaCierra() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Programada", 5)));

        assertThatThrownBy(() -> servicio.cerrar(1, 5, LocalTime.of(11, 0)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Solo se cierra una sesión en curso.");
    }

    @Test
    void validar_unaSesionCerradaPorOtro_laDejaValidadaConSuRevisor() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(31L);

        SesionVista vista = servicio.validar(1, 3);

        assertThat(vista.estado()).isEqualTo("Validada");
        ArgumentCaptor<SesionObservacion> guardada = ArgumentCaptor.forClass(SesionObservacion.class);
        verify(sesiones).save(guardada.capture());
        assertThat(guardada.getValue().getIdRevisor()).isEqualTo(3);
        assertThat(guardada.getValue().getFechaRevision()).isNotNull();
    }

    @Test
    void validar_laPropiaSesion_esProhibidoAunqueSeaSupervisor() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));

        assertThatThrownBy(() -> servicio.validar(1, 5))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No puedes validar una sesión que observaste tú.");
    }

    @Test
    void validar_sinSaberQuienValida_loRechaza() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));

        assertThatThrownBy(() -> servicio.validar(1, null))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Falta la identidad de quien valida");
    }

    @Test
    void validar_unaSesionQueSigueEnCurso_noLaValida() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "En curso", 5)));

        assertThatThrownBy(() -> servicio.validar(1, 3))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Solo se valida una sesión cerrada");
    }

    @Test
    void rechazar_unaSesionCerrada_guardaElMotivoYElRevisor() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));
        when(registros.countByIdSesionAndAnuladoFalse(1)).thenReturn(30L);

        servicio.rechazar(1, 3, "Los registros no cuadran con el conteo de la tienda");

        ArgumentCaptor<SesionObservacion> guardada = ArgumentCaptor.forClass(SesionObservacion.class);
        verify(sesiones).save(guardada.capture());
        assertThat(guardada.getValue().getEstado().getNombre()).isEqualTo("Rechazada");
        assertThat(guardada.getValue().getMotivoRechazo()).contains("no cuadran");
    }

    @Test
    void rechazar_laPropiaSesion_esProhibido() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));

        assertThatThrownBy(() -> servicio.rechazar(1, 5, "no sirve"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No puedes rechazar una sesión que observaste tú.");
    }

    @Test
    void rechazar_sinSaberQuienRechaza_loRechaza() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion(1, "Finalizada", 5)));

        assertThatThrownBy(() -> servicio.rechazar(1, null, "no sirve"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Falta la identidad de quien rechaza");
    }

    @Test
    void estado_siLaBaseNoTieneElEstadoProgramada_loDice() {
        when(estados.findByNombre("Programada")).thenReturn(Optional.empty());
        when(catalogo.bloqueDe(any())).thenReturn(4);

        assertThatThrownBy(() -> servicio.programar(peticion(), 3))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Falta el estado «Programada»");
    }
}
