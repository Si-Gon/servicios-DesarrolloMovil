package com.rinconsur.medicion.catalogo;

import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// La única llamada entre servicios, y es de lectura. Lo que importa: una hora cae en un bloque y
// el acceso tiene que existir; y si catálogos no responde, la medición no se cae por eso.
@DisplayName("Catálogos vistos desde medición")
class CatalogoServicioTest {

    private CatalogosCliente cliente;
    private CatalogoServicio servicio;

    @BeforeEach
    void preparar() {
        cliente = mock(CatalogosCliente.class);
        servicio = new CatalogoServicio(cliente);
    }

    private List<CatalogosCliente.BloqueDto> bloques() {
        return List.of(
            new CatalogosCliente.BloqueDto(3, LocalTime.of(9, 0), LocalTime.of(10, 0)),
            new CatalogosCliente.BloqueDto(4, LocalTime.of(10, 0), LocalTime.of(11, 0))
        );
    }

    @Test
    void bloqueDe_unaHoraDentroDeUnBloque_devuelveEseBloque() {
        when(cliente.bloques()).thenReturn(bloques());

        assertThat(servicio.bloqueDe(LocalTime.of(10, 30))).isEqualTo(4);
    }

    @Test
    void bloqueDe_laHoraDeInicio_cuentaParaElBloqueQueEmpieza() {
        when(cliente.bloques()).thenReturn(bloques());

        assertThat(servicio.bloqueDe(LocalTime.of(10, 0))).isEqualTo(4);
        assertThat(servicio.bloqueDe(LocalTime.of(9, 59))).isEqualTo(3);
    }

    @Test
    void bloqueDe_unaHoraQueNoCaeEnNingunBloque_loDiceConLaHora() {
        when(cliente.bloques()).thenReturn(bloques());

        assertThatThrownBy(() -> servicio.bloqueDe(LocalTime.of(22, 0)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("no cae dentro de ningún bloque");
    }

    @Test
    void bloqueDe_siElCatalogoNoResponde_avisaEnVezDeInventarUnBloque() {
        when(cliente.bloques()).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> servicio.bloqueDe(LocalTime.of(10, 0)))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No se pudo consultar el catálogo de bloques");
    }

    @Test
    void bloques_sePidenUnaVezYSeGuardanUnRatoEnMemoria() {
        when(cliente.bloques()).thenReturn(bloques());

        servicio.bloques();
        servicio.bloques();
        servicio.bloques();

        verify(cliente, times(1)).bloques();
    }

    @Test
    void exigirAcceso_conUnAccesoDelCatalogo_noHaceNada() {
        when(cliente.accesos()).thenReturn(List.of(
            new CatalogosCliente.AccesoDto(4, "A4", "Acceso 4", "Pasillo norte", null)));

        servicio.exigirAcceso(4);
    }

    @Test
    void exigirAcceso_conUnAccesoQueNoExiste_loRechaza() {
        when(cliente.accesos()).thenReturn(List.of(
            new CatalogosCliente.AccesoDto(4, "A4", "Acceso 4", "Pasillo norte", null)));

        assertThatThrownBy(() -> servicio.exigirAcceso(9))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El acceso 9 no existe en los catálogos.");
    }

    @Test
    void exigirAcceso_siElCatalogoNoResponde_dejaPasarLaProgramacion() {
        when(cliente.accesos()).thenThrow(new RuntimeException("timeout"));

        // Se prefiere programar sin validar que dejar al supervisor sin poder programar.
        servicio.exigirAcceso(4);
        assertThat(servicio.accesos()).isEmpty();
    }
}
