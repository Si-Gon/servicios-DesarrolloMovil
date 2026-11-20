package com.rinconsur.usuarios.dto;

import com.rinconsur.usuarios.model.Usuario;

// Lo que la app necesita para armar el menú y la ficha: nunca el hash ni la foto.
public record UsuarioVista(
    Integer id,
    String rut,
    String nombre,
    String cargo,
    String rol,
    String correo,
    String telefono,
    String genero,
    boolean activo,
    boolean debeCambiarClave
) {
    public static UsuarioVista de(Usuario u) {
        return new UsuarioVista(
            u.getId(), u.getRut(), u.nombreCompleto(), u.getCargo(), u.getRol().getCodigo(),
            u.getCorreo(), u.getTelefono(), u.getGenero(),
            Boolean.TRUE.equals(u.getActivo()), Boolean.TRUE.equals(u.getDebeCambiarClave())
        );
    }
}
