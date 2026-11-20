package com.rinconsur.medicion.catalogo;

import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// Los catálogos cambian poco: se guardan en memoria unos segundos para no preguntar en cada
// registro. Si catálogos no responde, se dice y se sigue: la medición no se cae por un desplegable.
@Service
public class CatalogoServicio {

    private static final Logger registro = LoggerFactory.getLogger(CatalogoServicio.class);
    private static final Duration VIGENCIA = Duration.ofSeconds(60);

    private final CatalogosCliente cliente;

    private List<CatalogosCliente.BloqueDto> bloques = List.of();
    private List<CatalogosCliente.AccesoDto> accesos = List.of();
    private Instant venceBloques = Instant.EPOCH;
    private Instant venceAccesos = Instant.EPOCH;

    public CatalogoServicio(CatalogosCliente cliente) {
        this.cliente = cliente;
    }

    public List<CatalogosCliente.BloqueDto> bloques() {
        if (Instant.now().isAfter(venceBloques)) {
            try {
                bloques = cliente.bloques();
                venceBloques = Instant.now().plus(VIGENCIA);
            } catch (Exception e) {
                registro.warn("No se pudo leer los bloques de catálogos: {}", e.getMessage());
            }
        }
        return bloques;
    }

    public List<CatalogosCliente.AccesoDto> accesos() {
        if (Instant.now().isAfter(venceAccesos)) {
            try {
                accesos = cliente.accesos();
                venceAccesos = Instant.now().plus(VIGENCIA);
            } catch (Exception e) {
                registro.warn("No se pudo leer los accesos de catálogos: {}", e.getMessage());
            }
        }
        return accesos;
    }

    // El bloque no se elige a mano: se deduce de la hora de inicio de la sesión.
    public Integer bloqueDe(LocalTime horaInicio) {
        List<CatalogosCliente.BloqueDto> lista = bloques();
        if (lista.isEmpty()) {
            throw ErrorDeNegocio.invalido(
                "No se pudo consultar el catálogo de bloques; intenta de nuevo en un momento.");
        }
        Optional<CatalogosCliente.BloqueDto> encontrado = lista.stream()
            .filter(b -> !horaInicio.isBefore(b.horaInicio()) && horaInicio.isBefore(b.horaFin()))
            .findFirst();
        return encontrado.map(CatalogosCliente.BloqueDto::id)
            .orElseThrow(() -> ErrorDeNegocio.invalido(
                "La hora " + horaInicio + " no cae dentro de ningún bloque horario."));
    }

    public void exigirAcceso(Integer idAcceso) {
        List<CatalogosCliente.AccesoDto> lista = accesos();
        if (lista.isEmpty()) {
            return; // catálogos no respondió: no se bloquea la programación por eso
        }
        boolean existe = lista.stream().anyMatch(a -> a.id().equals(idAcceso));
        if (!existe) {
            throw ErrorDeNegocio.invalido("El acceso " + idAcceso + " no existe en los catálogos.");
        }
    }
}
