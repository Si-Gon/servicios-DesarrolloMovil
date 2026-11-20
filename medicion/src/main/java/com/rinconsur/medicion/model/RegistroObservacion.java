package com.rinconsur.medicion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registro_observacion")
public class RegistroObservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer id;

    @Column(name = "id_sesion", nullable = false)
    private Integer idSesion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_clasificacion", nullable = false)
    private Integer idClasificacion;

    @Column(name = "id_rango", nullable = false)
    private Integer idRango;

    @Column(name = "id_tipo_compania", nullable = false)
    private Integer idTipoCompania;

    @Column(name = "compras_otra_tienda", nullable = false)
    private Boolean comprasOtraTienda = Boolean.FALSE;

    @Column(name = "id_acceso_destino")
    private Integer idAccesoDestino;

    @Column(nullable = false)
    private Boolean anulado = Boolean.FALSE;

    @Column(name = "id_usuario_anula")
    private Integer idUsuarioAnula;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;
}
