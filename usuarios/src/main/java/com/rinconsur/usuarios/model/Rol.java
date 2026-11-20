package com.rinconsur.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @Column(name = "id_rol")
    private Integer id;

    @Column(nullable = false, length = 5)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(length = 200)
    private String descripcion;
}
