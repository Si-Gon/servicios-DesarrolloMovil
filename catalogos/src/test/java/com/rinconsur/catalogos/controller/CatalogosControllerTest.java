package com.rinconsur.catalogos.controller;

import com.rinconsur.catalogos.excepcion.ManejadorDeErrores;
import com.rinconsur.catalogos.model.Acceso;
import com.rinconsur.catalogos.model.BloqueHorario;
import com.rinconsur.catalogos.model.Clasificacion;
import com.rinconsur.catalogos.model.RangoEtario;
import com.rinconsur.catalogos.model.TipoCompania;
import com.rinconsur.catalogos.repository.AccesoRepositorio;
import com.rinconsur.catalogos.repository.BloqueHorarioRepositorio;
import com.rinconsur.catalogos.repository.ClasificacionRepositorio;
import com.rinconsur.catalogos.repository.RangoEtarioRepositorio;
import com.rinconsur.catalogos.repository.TipoCompaniaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Los datos maestros: la app pide los desplegables y medición valida códigos con ellos. Nada
// se escribe por API, así que lo que hay que probar es que se lean completos y en orden.
@DisplayName("API de catálogos")
class CatalogosControllerTest {

    private AccesoRepositorio accesos;
    private BloqueHorarioRepositorio bloques;
    private ClasificacionRepositorio clasificaciones;
    private RangoEtarioRepositorio rangos;
    private TipoCompaniaRepositorio companias;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        accesos = mock(AccesoRepositorio.class);
        bloques = mock(BloqueHorarioRepositorio.class);
        clasificaciones = mock(ClasificacionRepositorio.class);
        rangos = mock(RangoEtarioRepositorio.class);
        companias = mock(TipoCompaniaRepositorio.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new CatalogosController(accesos, bloques, clasificaciones, rangos, companias))
            .setControllerAdvice(new ManejadorDeErrores())
            .build();
    }

    private Acceso acceso(int id, String codigo) {
        Acceso a = new Acceso();
        a.setId(id);
        a.setCodigo(codigo);
        a.setNombre("Acceso " + codigo);
        a.setUbicacion("Pasillo norte");
        return a;
    }

    @Test
    void accesos_devuelveLosCuatroAccesosDeLaTienda() throws Exception {
        when(accesos.findAllByOrderByIdAsc())
            .thenReturn(List.of(acceso(1, "A1"), acceso(2, "A2"), acceso(3, "A3"), acceso(4, "A4")));

        mvc.perform(get("/api/catalogos/accesos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[3].codigo").value("A4"))
            .andExpect(jsonPath("$[0].ubicacion").value("Pasillo norte"));
    }

    @Test
    void bloques_devuelveLosOnceBloquesDelDia() throws Exception {
        BloqueHorario b = new BloqueHorario();
        b.setId(4);
        b.setHoraInicio(LocalTime.of(13, 0));
        b.setHoraFin(LocalTime.of(14, 0));
        when(bloques.findAllByOrderByIdAsc()).thenReturn(List.of(b));

        mvc.perform(get("/api/catalogos/bloques"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(4))
            .andExpect(jsonPath("$[0].horaInicio").value("13:00:00"));
    }

    @Test
    void clasificaciones_devuelveVisitaTransitoYNoDeterminado() throws Exception {
        Clasificacion c = new Clasificacion();
        c.setId(1);
        c.setNombre("Transito");
        when(clasificaciones.findAllByOrderByIdAsc()).thenReturn(List.of(c));

        mvc.perform(get("/api/catalogos/clasificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Transito"));
    }

    @Test
    void rangos_devuelveLosRangosEnElOrdenDelAnexo() throws Exception {
        RangoEtario r = new RangoEtario();
        r.setId(2);
        r.setNombre("De 18 a 25 años");
        r.setOrden(2);
        when(rangos.findAllByOrderByOrdenAsc()).thenReturn(List.of(r));

        mvc.perform(get("/api/catalogos/rangos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("De 18 a 25 años"));
    }

    @Test
    void companias_devuelveLosTiposDeCompania() throws Exception {
        TipoCompania t = new TipoCompania();
        t.setId(1);
        t.setNombre("Solo");
        when(companias.findAllByOrderByIdAsc()).thenReturn(List.of(t));

        mvc.perform(get("/api/catalogos/companias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Solo"));
    }

    @Test
    void accesoPorCodigo_conUnCodigoQueExiste_loDevuelve() throws Exception {
        when(accesos.findByCodigo("A4")).thenReturn(Optional.of(acceso(4, "A4")));

        mvc.perform(get("/api/catalogos/accesos/A4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void accesoPorCodigo_conUnCodigoQueNoExiste_responde404ConMotivo() throws Exception {
        when(accesos.findByCodigo("A9")).thenReturn(Optional.empty());

        mvc.perform(get("/api/catalogos/accesos/A9"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.motivo").value("No existe un acceso con el código A9."));
    }
}
