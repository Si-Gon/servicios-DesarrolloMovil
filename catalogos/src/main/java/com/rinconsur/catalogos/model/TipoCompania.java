package com.rinconsur.catalogos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_compania")
public class TipoCompania {

    @Id
    @Column(name = "id_tipo_compania")
    private Integer id;

    @Column(nullable = false, length = 40)
    private String nombre;
}
