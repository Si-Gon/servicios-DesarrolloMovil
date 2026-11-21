package com.rinconsur.medicion.registro;

import com.rinconsur.medicion.dto.RegistrarPeticion;
import com.rinconsur.medicion.dto.RegistroVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.model.RegistroObservacion;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// El camino crítico del proyecto: cada toque escribe una fila. No consulta a otros servicios
// ni espera a nadie, porque quien anota está de pie en el pasillo contando gente.
@Service
public class RegistroServicio {

    private final RegistroRepositorio registros;
    private final SesionRepositorio sesiones;

    public RegistroServicio(RegistroRepositorio registros, SesionRepositorio sesiones) {
        this.registros = registros;
        this.sesiones = sesiones;
    }

    @Transactional
    public RegistroVista registrar(RegistrarPeticion peticion, Integer idObservador) {
        SesionObservacion s = sesiones.findById(peticion.idSesion())
            .orElseThrow(() -> ErrorDeNegocio.noEncontrado("No existe esa sesión."));
        if (!"En curso".equalsIgnoreCase(s.getEstado().getNombre())) {
            throw ErrorDeNegocio.conflicto(
                "La sesión no está en curso: ahora figura como «" + s.getEstado().getNombre() + "».");
        }
        if (idObservador != null && !idObservador.equals(s.getIdObservador())) {
            throw ErrorDeNegocio.prohibido("Solo quien observa la sesión puede anotar en ella.");
        }

        RegistroObservacion r = new RegistroObservacion();
        r.setIdSesion(peticion.idSesion());
        r.setFechaHora(peticion.fechaHora() == null ? LocalDateTime.now() : peticion.fechaHora());
        r.setIdClasificacion(peticion.idClasificacion());
        r.setIdRango(peticion.idRango());
        r.setIdTipoCompania(peticion.idTipoCompania());
        r.setComprasOtraTienda(Boolean.TRUE.equals(peticion.comprasOtraTienda()));
        r.setIdAccesoDestino(peticion.idAccesoDestino());
        r.setAnulado(Boolean.FALSE);
        return RegistroVista.de(registros.save(r));
    }

    // Anular no borra: deja la marca, quién y por qué. El registro sigue en la base y deja de
    // contar para los indicadores.
    @Transactional
    public RegistroVista anular(Integer idRegistro, Integer idUsuario, String motivo) {
        if (idUsuario == null) {
            throw ErrorDeNegocio.invalido("Falta la identidad de quien anula el registro.");
        }
        RegistroObservacion r = registros.findById(idRegistro)
            .orElseThrow(() -> ErrorDeNegocio.noEncontrado("No existe ese registro."));
        if (Boolean.TRUE.equals(r.getAnulado())) {
            throw ErrorDeNegocio.conflicto("Ese registro ya estaba anulado.");
        }
        r.setAnulado(Boolean.TRUE);
        r.setIdUsuarioAnula(idUsuario);
        r.setFechaAnulacion(LocalDateTime.now());
        r.setMotivoAnulacion(motivo);
        return RegistroVista.de(registros.save(r));
    }

    @Transactional(readOnly = true)
    public List<RegistroVista> ultimos(Integer idSesion) {
        return registros.findTop10ByIdSesionAndAnuladoFalseOrderByFechaHoraDesc(idSesion).stream()
            .map(RegistroVista::de)
            .toList();
    }
}
