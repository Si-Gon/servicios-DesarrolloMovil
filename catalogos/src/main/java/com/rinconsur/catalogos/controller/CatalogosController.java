package com.rinconsur.catalogos.controller;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Lectura de los datos maestros. La app los pide para sus desplegables y el servicio de
// medición los usa para validar códigos: nadie los escribe por API, vienen de la migración.
@RestController
@RequestMapping("/api")
@Tag(name = "Catálogos", description = "Datos maestros de la tienda")
public class CatalogosController {

    private final AccesoRepositorio accesos;
    private final BloqueHorarioRepositorio bloques;
    private final ClasificacionRepositorio clasificaciones;
    private final RangoEtarioRepositorio rangos;
    private final TipoCompaniaRepositorio companias;

    public CatalogosController(AccesoRepositorio accesos, BloqueHorarioRepositorio bloques,
                               ClasificacionRepositorio clasificaciones, RangoEtarioRepositorio rangos,
                               TipoCompaniaRepositorio companias) {
        this.accesos = accesos;
        this.bloques = bloques;
        this.clasificaciones = clasificaciones;
        this.rangos = rangos;
        this.companias = companias;
    }

    @GetMapping("/catalogos/accesos")
    @Operation(summary = "Los cuatro accesos de la tienda (A1 a A4)")
    public List<Acceso> accesos() {
        return accesos.findAllByOrderByIdAsc();
    }

    @GetMapping("/catalogos/bloques")
    @Operation(summary = "Los once bloques horarios del día")
    public List<BloqueHorario> bloques() {
        return bloques.findAllByOrderByIdAsc();
    }

    @GetMapping("/catalogos/clasificaciones")
    @Operation(summary = "Visita, tránsito y no determinado")
    public List<Clasificacion> clasificaciones() {
        return clasificaciones.findAllByOrderByIdAsc();
    }

    @GetMapping("/catalogos/rangos")
    @Operation(summary = "Rangos etarios del anexo, en orden")
    public List<RangoEtario> rangos() {
        return rangos.findAllByOrderByOrdenAsc();
    }

    @GetMapping("/catalogos/companias")
    @Operation(summary = "Solo, pareja, familia y grupo")
    public List<TipoCompania> companias() {
        return companias.findAllByOrderByIdAsc();
    }

    @GetMapping("/catalogos/accesos/{codigo}")
    @Operation(summary = "Un acceso por su código (A1 a A4)")
    public Acceso accesoPorCodigo(@PathVariable String codigo) {
        return accesos.findByCodigo(codigo)
            .orElseThrow(() -> com.rinconsur.catalogos.excepcion.ErrorDeNegocio
                .noEncontrado("No existe un acceso con el código " + codigo + "."));
    }
}
