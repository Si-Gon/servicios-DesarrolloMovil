package com.rinconsur.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Column(nullable = false, length = 12)
    private String rut;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false, length = 40)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 40)
    private String apellidoMaterno;

    @Column(nullable = false, length = 1)
    private String genero;

    @Column(nullable = false, length = 60)
    private String cargo;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(length = 20)
    private String telefono;

    // No viaja nunca en las respuestas: el hash se queda en el servicio.
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "debe_cambiar_clave", nullable = false)
    private Boolean debeCambiarClave = Boolean.FALSE;

    @Column(name = "codigo_vence")
    private LocalDateTime codigoVence;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @JsonIgnore
    @Column(name = "foto")
    private byte[] foto;

    // La pone MySQL con su DEFAULT CURRENT_TIMESTAMP: la entidad no la escribe.
    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public String nombreCompleto() {
        return (nombres + " " + apellidoPaterno).trim();
    }
}
