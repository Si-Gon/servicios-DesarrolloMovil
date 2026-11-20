package com.rinconsur.catalogos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rango_etario")
public class RangoEtario {

    @Id
    @Column(name = "id_rango")
    private Integer id;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(name = "edad_min", nullable = false)
    private Integer edadMin;

    @Column(name = "edad_max")
    private Integer edadMax;

    @Column(nullable = false)
    private Integer orden;
}
