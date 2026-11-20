package com.rinconsur.medicion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conteo_ingreso")
public class ConteoIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conteo")
    private Integer id;

    // Nulo = el bloque se cargó como total del local, sin repartir entre accesos.
    @Column(name = "id_acceso")
    private Integer idAcceso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "id_bloque", nullable = false)
    private Integer idBloque;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "id_usuario_carga", nullable = false)
    private Integer idUsuarioCarga;

    @Column(name = "fecha_carga", insertable = false, updatable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "id_usuario_correccion")
    private Integer idUsuarioCorreccion;

    @Column(name = "fecha_correccion")
    private LocalDateTime fechaCorreccion;
}
