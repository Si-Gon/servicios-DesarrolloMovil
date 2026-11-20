package com.rinconsur.catalogos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "bloque_horario")
public class BloqueHorario {

    @Id
    @Column(name = "id_bloque")
    private Integer id;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
