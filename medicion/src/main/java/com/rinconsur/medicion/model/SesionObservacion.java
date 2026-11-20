package com.rinconsur.medicion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "sesion_observacion")
public class SesionObservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codigo;

    // El acceso y el observador viven en otros servicios: acá son columnas simples, sin clave
    // foránea, porque cada base es de su dueño.
    @Column(name = "id_acceso", nullable = false)
    private Integer idAcceso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio_prog", nullable = false)
    private LocalTime horaInicioProg;

    @Column(name = "hora_fin_prog", nullable = false)
    private LocalTime horaFinProg;

    @Column(name = "id_bloque", nullable = false)
    private Integer idBloque;

    @Column(name = "observador_nombre", length = 120)
    private String observadorNombre;

    @Column(name = "inicio_real")
    private LocalDateTime inicioReal;

    @Column(name = "fin_real")
    private LocalDateTime finReal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado")
    private EstadoSesion estado;

    @Column(name = "id_observador", nullable = false)
    private Integer idObservador;

    @Column(name = "id_programador", nullable = false)
    private Integer idProgramador;

    @Column(name = "id_revisor")
    private Integer idRevisor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;
}
