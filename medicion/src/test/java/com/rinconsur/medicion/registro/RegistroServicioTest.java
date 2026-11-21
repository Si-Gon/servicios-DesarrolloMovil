package com.rinconsur.medicion.registro;

import com.rinconsur.medicion.dto.RegistrarPeticion;
import com.rinconsur.medicion.dto.RegistroVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.model.EstadoSesion;
import com.rinconsur.medicion.model.RegistroObservacion;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cada toque escribe una fila: es el camino crítico y no puede depender de nada externo. Las
// reglas son dos: la sesión tiene que estar en curso y solo anota quien la observa.
@DisplayName("Registro de personas observadas")
class RegistroServicioTest {

    private RegistroRepositorio registros;
    private SesionRepositorio sesiones;
    private RegistroServicio servicio;

    @BeforeEach
    void preparar() {
        registros = mock(RegistroRepositorio.class);
        sesiones = mock(SesionRepositorio.class);
        servicio = new RegistroServicio(registros, sesiones);
        when(registros.save(any(RegistroObservacion.class))).thenAnswer(i -> {
            RegistroObservacion r = i.getArgument(0);
            r.setId(99);
            return r;
        });
    }

    private SesionObservacion sesion(String nombreEstado, int idObservador) {
        EstadoSesion e = new EstadoSesion();
        e.setId(2);
        e.setNombre(nombreEstado);
        SesionObservacion s = new SesionObservacion();
        s.setId(1);
        s.setCodigo("S-0001");
        s.setFecha(LocalDate.of(2026, 9, 15));
        s.setIdObservador(idObservador);
        s.setEstado(e);
        return s;
    }

    private RegistrarPeticion peticion(LocalDateTime cuando) {
        return new RegistrarPeticion(1, 1, 4, 2, false, 2, cuando);
    }

    @Test
    void registrar_enUnaSesionEnCurso_guardaLaHoraQueMandoElTelefono() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion("En curso", 5)));
        LocalDateTime anotado = LocalDateTime.of(2026, 9, 15, 13, 5);

        RegistroVista vista = servicio.registrar(peticion(anotado), 5);

        assertThat(vista.id()).isEqualTo(99);
        ArgumentCaptor<RegistroObservacion> guardado = ArgumentCaptor.forClass(RegistroObservacion.class);
        verify(registros).save(guardado.capture());
        // La hora es la del toque, no la del envío: un registro sin red puede llegar minutos después.
        assertThat(guardado.getValue().getFechaHora()).isEqualTo(anotado);
        assertThat(guardado.getValue().getAnulado()).isFalse();
    }

    @Test
    void registrar_sinHoraDelTelefono_usaLaHoraDelServidor() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion("En curso", 5)));

        servicio.registrar(peticion(null), 5);

        ArgumentCaptor<RegistroObservacion> guardado = ArgumentCaptor.forClass(RegistroObservacion.class);
        verify(registros).save(guardado.capture());
        assertThat(guardado.getValue().getFechaHora()).isNotNull();
    }

    @Test
    void registrar_enUnaSesionCerrada_laRechazaConElEstadoReal() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion("Finalizada", 5)));

        assertThatThrownBy(() -> servicio.registrar(peticion(null), 5))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("no está en curso")
            .hasMessageContaining("Finalizada");

        verify(registros, never()).save(any(RegistroObservacion.class));
    }

    @Test
    void registrar_enLaSesionDeOtraPersona_esProhibido() {
        when(sesiones.findById(1)).thenReturn(Optional.of(sesion("En curso", 5)));

        assertThatThrownBy(() -> servicio.registrar(peticion(null), 9))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Solo quien observa la sesión puede anotar en ella.");
    }

    @Test
    void registrar_enUnaSesionQueNoExiste_da404() {
        when(sesiones.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(peticion(null), 5))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No existe esa sesión.");
    }

    @Test
    void anular_unRegistroVigente_loMarcaSinBorrarlo() {
        RegistroObservacion r = new RegistroObservacion();
        r.setId(7);
        r.setIdSesion(1);
        r.setAnulado(false);
        when(registros.findById(7)).thenReturn(Optional.of(r));

        RegistroVista vista = servicio.anular(7, 3, "Se anotó dos veces la misma persona");

        assertThat(vista.anulado()).isTrue();
        assertThat(r.getIdUsuarioAnula()).isEqualTo(3);
        assertThat(r.getFechaAnulacion()).isNotNull();
        assertThat(r.getMotivoAnulacion()).contains("dos veces");
    }

    @Test
    void anular_unRegistroYaAnulado_noLoAnulaDosVeces() {
        RegistroObservacion r = new RegistroObservacion();
        r.setId(7);
        r.setAnulado(true);
        when(registros.findById(7)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> servicio.anular(7, 3, "otra vez"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("ya estaba anulado");
    }

    @Test
    void anular_sinSaberQuienAnula_loRechaza() {
        assertThatThrownBy(() -> servicio.anular(7, null, "motivo"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Falta la identidad de quien anula");
    }

    @Test
    void anular_unRegistroQueNoExiste_da404() {
        when(registros.findById(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.anular(77, 3, "motivo"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No existe ese registro.");
    }

    @Test
    void ultimos_devuelveLosRegistrosDeLaSesionComoVista() {
        RegistroObservacion r = new RegistroObservacion();
        r.setId(10);
        r.setIdSesion(1);
        r.setIdClasificacion(2);
        r.setIdRango(4);
        r.setIdTipoCompania(1);
        r.setComprasOtraTienda(true);
        r.setAnulado(false);
        when(registros.findTop10ByIdSesionAndAnuladoFalseOrderByFechaHoraDesc(1)).thenReturn(List.of(r));

        List<RegistroVista> lista = servicio.ultimos(1);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).idClasificacion()).isEqualTo(2);
        assertThat(lista.get(0).comprasOtraTienda()).isTrue();
    }
}
