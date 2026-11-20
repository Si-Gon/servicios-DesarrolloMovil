package com.rinconsur.catalogos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "acceso")
public class Acceso {

    @Id
    @Column(name = "id_acceso")
    private Integer id;

    @Column(nullable = false, length = 2)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String ubicacion;

    @Column(length = 200)
    private String observacion;
}
