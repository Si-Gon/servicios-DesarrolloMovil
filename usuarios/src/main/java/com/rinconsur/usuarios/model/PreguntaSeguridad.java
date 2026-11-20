package com.rinconsur.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pregunta_seguridad")
public class PreguntaSeguridad {

    @Id
    @Column(name = "id_pregunta")
    private Integer id;

    @Column(nullable = false, length = 120)
    private String texto;

    @Column(nullable = false)
    private Boolean activa = Boolean.TRUE;
}
