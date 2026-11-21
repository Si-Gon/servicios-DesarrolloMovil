package com.rinconsur.medicion.controller;

import com.rinconsur.medicion.excepcion.ManejadorDeErrores;
import com.rinconsur.medicion.repository.IndicadorRepositorio;
import com.rinconsur.medicion.repository.ReporteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Los números del anexo y el informe mensual. Dos cosas se prueban acá que ya fallaron en
// producción: las columnas nulas de una vista (SUM sobre un bloque sin datos) y el mes sin cifras.
@DisplayName("API de indicadores y reporte")
class IndicadoresYReporteControllerTest {

    private IndicadorRepositorio indicadores;
    private ReporteRepositorio reporte;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        indicadores = mock(IndicadorRepositorio.class);
        reporte = mock(ReporteRepositorio.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new IndicadoresController(indicadores),
                new ReporteController(reporte))
            .setControllerAdvice(new ManejadorDeErrores())
            .build();
    }

    // 12 columnas: fecha, tipo de día y los ocho números del día.
    private Object[] filaDelDia() {
        return new Object[]{
            LocalDate.of(2026, 9, 15), "Habil",
            5735L, 497L, new BigDecimal("8.7"), "Con medición",
            new BigDecimal("54.0"), new BigDecimal("2638"), new BigDecimal("4.3"),
            5, 4, 2
        };
    }

    // 13 columnas: el agregado trae desde y hasta antes del tipo de día.
    private Object[] filaDelTipo() {
        return new Object[]{
            LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20), "Habil",
            5735L, 497L, new BigDecimal("8.7"), "Con medición",
            new BigDecimal("54.0"), new BigDecimal("2638"), new BigDecimal("4.3"),
            5, 4, 2
        };
    }

    @Test
    void delDia_deUnDiaConCifras_devuelveLosNumerosDelAnexo() throws Exception {
        when(indicadores.filaDelDia(LocalDate.of(2026, 9, 15))).thenReturn(List.<Object[]>of(filaDelDia()));

        mvc.perform(get("/api/indicadores/2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ingresos").value(5735))
            .andExpect(jsonPath("$.transacciones").value(497))
            .andExpect(jsonPath("$.conversionBruta").value(8.7))
            .andExpect(jsonPath("$.tipoDia").value("Habil"))
            .andExpect(jsonPath("$.medidos").value(5));
    }

    @Test
    void delDia_deUnDiaSinCifras_devuelveVacioParaQueLaAppCaigaALoLocal() throws Exception {
        when(indicadores.filaDelDia(any())).thenReturn(List.of());

        mvc.perform(get("/api/indicadores/2026-11-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void delTipo_sinFechas_agregaConExtremosAbiertosParaNoMandarNulosAlSql() throws Exception {
        when(indicadores.filaDelTipo(eq("Habil"), any(), any())).thenReturn(List.<Object[]>of(filaDelTipo()));

        mvc.perform(get("/api/indicadores").param("tipoDia", "Habil"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.desde").value("2026-09-14"))
            .andExpect(jsonPath("$.hasta").value("2026-09-20"));

        verify(indicadores).filaDelTipo("Habil", LocalDate.of(1900, 1, 1), LocalDate.of(2999, 12, 31));
    }

    @Test
    void delTipo_deUnaSemanaConcreta_pasaElRangoPedido() throws Exception {
        when(indicadores.filaDelTipo(eq("Habil"), any(), any())).thenReturn(List.<Object[]>of(filaDelTipo()));

        mvc.perform(get("/api/indicadores")
                .param("tipoDia", "Habil")
                .param("desde", "2026-09-14")
                .param("hasta", "2026-09-20"))
            .andExpect(status().isOk());

        verify(indicadores).filaDelTipo("Habil", LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
    }

    @Test
    void pendientes_listaLosBloquesSinMedirConSusDiasDeAtraso() throws Exception {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{1, LocalDate.of(2026, 9, 15), 4, LocalDate.of(2026, 9, 1), 14, 2.0});
        when(indicadores.pendientesDeMedir(any())).thenReturn(filas);

        mvc.perform(get("/api/indicadores/pendientes/2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].idAcceso").value(1))
            .andExpect(jsonPath("$[0].idBloque").value(4))
            .andExpect(jsonPath("$[0].diasSinMedir").value(14));
    }

    @Test
    void pendientes_sinUltimaSesionValida_noMandaUnNuloAlCliente() throws Exception {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{2, LocalDate.of(2026, 9, 15), 7, null, null, null});
        when(indicadores.pendientesDeMedir(any())).thenReturn(filas);

        mvc.perform(get("/api/indicadores/pendientes/2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ultimaSesionValida").value(""))
            .andExpect(jsonPath("$[0].diasSinMedir").value(0));
    }

    private List<Object[]> avanceMes(int dias, String conversion) {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{dias, new BigDecimal(conversion)});
        return filas;
    }

    private List<Object[]> meses() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"2026-10", 4780L, 415L, new BigDecimal("8.7"), 1});
        filas.add(new Object[]{"2026-09", 5735L, 497L, new BigDecimal("8.7"), 1});
        return filas;
    }

    private List<Object[]> grupos() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"3", 46L, new BigDecimal("21.1")});
        filas.add(new Object[]{"4", 45L, new BigDecimal("20.6")});
        return filas;
    }

    @Test
    void reporte_delMesConCifras_armaLaMetaYLosRepartos() throws Exception {
        when(reporte.avanceDelMes("2026-10")).thenReturn(avanceMes(1, "8.7"));
        when(reporte.metaDelMes(2026, 10)).thenReturn(List.of(new BigDecimal("10.0")));
        when(reporte.meses()).thenReturn(meses());
        when(reporte.horas("2026-10")).thenReturn(List.<Object[]>of(new Object[]{4, 587L, 180L, 1L, 8L}));
        when(reporte.porRango("2026-10")).thenReturn(grupos());
        when(reporte.porCompania("2026-10")).thenReturn(grupos());
        when(reporte.compras("2026-10")).thenReturn(List.<Object[]>of(new Object[]{"si", 20L, new BigDecimal("50.0")}));

        mvc.perform(get("/api/indicadores/reporte/2026-10-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.anio").value(2026))
            .andExpect(jsonPath("$.meta.mes").value(10))
            .andExpect(jsonPath("$.meta.objetivo").value(10.0))
            .andExpect(jsonPath("$.meta.actual").value(8.7))
            .andExpect(jsonPath("$.meses[0].mes").value("2026-10"))
            .andExpect(jsonPath("$.horas[0].transito").value(180))
            .andExpect(jsonPath("$.porRango[0].cantidad").value(46))
            .andExpect(jsonPath("$.compras[0].etiqueta").value("si"));
    }

    @Test
    void reporte_deUnMesSinCifras_informaElUltimoMesConDatos() throws Exception {
        // Noviembre no tiene carga: en vez de un informe en cero se cae a octubre y se dice cuál es.
        when(reporte.avanceDelMes("2026-11")).thenReturn(avanceMes(0, "0"));
        when(reporte.ultimoMesConDatos()).thenReturn("2026-10");
        when(reporte.avanceDelMes("2026-10")).thenReturn(avanceMes(1, "8.7"));
        when(reporte.metaDelMes(2026, 10)).thenReturn(List.of(new BigDecimal("10.0")));
        when(reporte.meses()).thenReturn(meses());
        when(reporte.horas(anyString())).thenReturn(List.<Object[]>of(new Object[]{4, 587L, 180L, 1L, 8L}));
        when(reporte.porRango(anyString())).thenReturn(grupos());
        when(reporte.porCompania(anyString())).thenReturn(grupos());
        when(reporte.compras(anyString())).thenReturn(grupos());

        mvc.perform(get("/api/indicadores/reporte/2026-11-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.mes").value(10))
            .andExpect(jsonPath("$.meta.dias").value(1));
    }

    @Test
    void reporte_conSumasNulasEnLaVista_noRevientaCon500() throws Exception {
        // Un bloque sin información devuelve NULL en SUM: se traduce a cero en vez de dejar caer
        // la petición entera, que es lo que pasaba antes.
        when(reporte.avanceDelMes("2026-09")).thenReturn(avanceMes(1, "8.7"));
        when(reporte.metaDelMes(2026, 9)).thenReturn(new ArrayList<>());
        when(reporte.meses()).thenReturn(List.<Object[]>of(new Object[]{"2026-09", null, null, null, 1}));
        when(reporte.horas(anyString())).thenReturn(List.<Object[]>of(new Object[]{4, null, null, null, null}));
        when(reporte.porRango(anyString())).thenReturn(List.<Object[]>of(new Object[]{"2", null, null}));
        when(reporte.porCompania(anyString())).thenReturn(List.of());
        when(reporte.compras(anyString())).thenReturn(List.of());

        mvc.perform(get("/api/indicadores/reporte/2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.objetivo").doesNotExist())
            .andExpect(jsonPath("$.meses[0].ingresos").value(0))
            .andExpect(jsonPath("$.horas[0].transito").value(0))
            .andExpect(jsonPath("$.porRango[0].cantidad").value(0));
    }
}
