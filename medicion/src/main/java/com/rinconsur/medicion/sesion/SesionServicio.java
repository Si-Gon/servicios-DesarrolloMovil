package com.rinconsur.medicion.sesion;

import com.rinconsur.medicion.catalogo.CatalogoServicio;
import com.rinconsur.medicion.dto.ProgramarSesionPeticion;
import com.rinconsur.medicion.dto.SesionVista;
import com.rinconsur.medicion.excepcion.ErrorDeNegocio;
import com.rinconsur.medicion.model.SesionObservacion;
import com.rinconsur.medicion.repository.EstadoSesionRepositorio;
import com.rinconsur.medicion.repository.RegistroRepositorio;
import com.rinconsur.medicion.repository.SesionRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

// Las reglas de la sesión viven acá: el controlador solo traduce HTTP. Así la misma regla
// sirve para la app, para Swagger y para una prueba.
@Service
public class SesionServicio {

    // El caso pide un mínimo de treinta registros para aceptar una sesión como muestra.
    private static final int MINIMO_REGISTROS = 30;

    private final SesionRepositorio sesiones;
    private final RegistroRepositorio registros;
    private final EstadoSesionRepositorio estados;
    private final CatalogoServicio catalogo;

    public SesionServicio(SesionRepositorio sesiones, RegistroRepositorio registros,
                          EstadoSesionRepositorio estados, CatalogoServicio catalogo) {
        this.sesiones = sesiones;
        this.registros = registros;
        this.estados = estados;
        this.catalogo = catalogo;
    }

    @Transactional
    public SesionVista programar(ProgramarSesionPeticion peticion, Integer idProgramador) {
        if (idProgramador == null) {
            throw ErrorDeNegocio.invalido("Falta la identidad de quien programa la sesión.");
        }
        if (!peticion.horaFin().isAfter(peticion.horaInicio())) {
            throw ErrorDeNegocio.invalido("La hora de término tiene que ser posterior a la de inicio.");
        }
        catalogo.exigirAcceso(peticion.idAcceso());
        Integer idBloque = catalogo.bloqueDe(peticion.horaInicio());

        SesionObservacion s = new SesionObservacion();
        s.setCodigo(siguienteCodigo());
        s.setIdAcceso(peticion.idAcceso());
        s.setFecha(peticion.fecha());
        s.setHoraInicioProg(peticion.horaInicio());
        s.setHoraFinProg(peticion.horaFin());
        s.setIdBloque(idBloque);
        s.setIdObservador(peticion.idObservador());
        s.setObservadorNombre(peticion.observadorNombre());
        s.setIdProgramador(idProgramador);
        s.setEstado(estado("Programada"));
        SesionObservacion guardada = sesiones.save(s);
        return SesionVista.de(guardada, 0);
    }

    @Transactional
    public SesionVista iniciar(Integer id, Integer idObservador, LocalTime hora) {
        SesionObservacion s = buscar(id);
        exigirEstado(s, "Programada", "Solo se inicia una sesión programada.");
        if (idObservador != null && !idObservador.equals(s.getIdObservador())) {
            throw ErrorDeNegocio.prohibido("Esta sesión está asignada a otra persona.");
        }
        s.setEstado(estado("En curso"));
        s.setInicioReal(LocalDateTime.of(s.getFecha(), hora));
        SesionObservacion guardada = sesiones.save(s);
        return SesionVista.de(guardada, registros.countByIdSesionAndAnuladoFalse(id));
    }

    // Al cerrar se revisa el mínimo del caso: una sesión con menos registros no es muestra.
    @Transactional
    public SesionVista cerrar(Integer id, Integer idObservador, LocalTime hora) {
        SesionObservacion s = buscar(id);
        exigirEstado(s, "En curso", "Solo se cierra una sesión en curso.");
        if (idObservador != null && !idObservador.equals(s.getIdObservador())) {
            throw ErrorDeNegocio.prohibido("Esta sesión está asignada a otra persona.");
        }
        long validos = registros.countByIdSesionAndAnuladoFalse(id);
        if (validos < MINIMO_REGISTROS) {
            throw ErrorDeNegocio.invalido("La sesión tiene " + validos + " registros y el mínimo es "
                + MINIMO_REGISTROS + ". Sigue midiendo o anula la sesión.");
        }
        s.setEstado(estado("Finalizada"));
        s.setFinReal(LocalDateTime.of(s.getFecha(), hora));
        SesionObservacion guardada = sesiones.save(s);
        return SesionVista.de(guardada, validos);
    }

    // Un supervisor no valida su propia sesión: la regla está también en la tabla.
    @Transactional
    public SesionVista validar(Integer id, Integer idRevisor) {
        SesionObservacion s = buscar(id);
        exigirEstado(s, "Finalizada", "Solo se valida una sesión cerrada por el observador.");
        if (idRevisor == null) {
            throw ErrorDeNegocio.invalido("Falta la identidad de quien valida la sesión.");
        }
        if (idRevisor.equals(s.getIdObservador())) {
            throw ErrorDeNegocio.prohibido("No puedes validar una sesión que observaste tú.");
        }
        s.setEstado(estado("Validada"));
        s.setIdRevisor(idRevisor);
        s.setFechaRevision(LocalDateTime.now());
        SesionObservacion guardada = sesiones.save(s);
        return SesionVista.de(guardada, registros.countByIdSesionAndAnuladoFalse(id));
    }

    @Transactional
    public SesionVista rechazar(Integer id, Integer idRevisor, String motivo) {
        SesionObservacion s = buscar(id);
        exigirEstado(s, "Finalizada", "Solo se rechaza una sesión cerrada por el observador.");
        if (idRevisor == null) {
            throw ErrorDeNegocio.invalido("Falta la identidad de quien rechaza la sesión.");
        }
        if (idRevisor.equals(s.getIdObservador())) {
            throw ErrorDeNegocio.prohibido("No puedes rechazar una sesión que observaste tú.");
        }
        s.setEstado(estado("Rechazada"));
        s.setIdRevisor(idRevisor);
        s.setFechaRevision(LocalDateTime.now());
        s.setMotivoRechazo(motivo);
        SesionObservacion guardada = sesiones.save(s);
        return SesionVista.de(guardada, registros.countByIdSesionAndAnuladoFalse(id));
    }

    @Transactional(readOnly = true)
    public SesionObservacion buscar(Integer id) {
        return sesiones.findById(id)
            .orElseThrow(() -> ErrorDeNegocio.noEncontrado("No existe una sesión con ese identificador."));
    }

    // La sesión tiene que estar en el estado que la operación espera: si cambió entre que la
    // pantalla se abrió y se tocó el botón, se avisa con el estado real.
    private void exigirEstado(SesionObservacion s, String esperado, String motivo) {
        if (!esperado.equalsIgnoreCase(s.getEstado().getNombre())) {
            throw ErrorDeNegocio.conflicto(motivo + " Ahora está en «" + s.getEstado().getNombre() + "».");
        }
    }

    private com.rinconsur.medicion.model.EstadoSesion estado(String nombre) {
        return estados.findByNombre(nombre)
            .orElseThrow(() -> ErrorDeNegocio.invalido("Falta el estado «" + nombre + "» en la base."));
    }

    // Código legible y único: S-000N, sin repetir el que ya está tomado.
    private String siguienteCodigo() {
        long siguiente = sesiones.count() + 1;
        String codigo;
        do {
            codigo = String.format("S-%04d", siguiente++);
        } while (sesiones.existsByCodigo(codigo));
        return codigo;
    }
}
