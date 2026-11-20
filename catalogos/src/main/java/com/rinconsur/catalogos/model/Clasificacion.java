package com.rinconsur.catalogos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "clasificacion")
public class Clasificacion {

    @Id
    @Column(name = "id_clasificacion")
    private Integer id;

    @Column(nullable = false, length = 30)
    private String nombre;
}
