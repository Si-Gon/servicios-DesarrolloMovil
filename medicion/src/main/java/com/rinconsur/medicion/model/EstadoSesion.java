package com.rinconsur.medicion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "estado_sesion")
public class EstadoSesion {

    @Id
    @Column(name = "id_estado")
    private Integer id;

    @Column(nullable = false, length = 30)
    private String nombre;
}
