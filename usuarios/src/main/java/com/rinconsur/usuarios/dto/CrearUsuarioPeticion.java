package com.rinconsur.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// El administrador no elige clave: el servicio genera un código temporal y la persona
// define su clave y su pregunta en el primer acceso.
public record CrearUsuarioPeticion(
    @NotBlank(message = "El RUT es obligatorio.") String rut,
    @NotBlank(message = "Los nombres son obligatorios.") String nombres,
    @NotBlank(message = "El apellido paterno es obligatorio.") String apellidoPaterno,
    String apellidoMaterno,
    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato válido.") String correo,
    @Size(max = 20, message = "El teléfono es demasiado largo.") String telefono,
    @Pattern(regexp = "[FM]", message = "Indica si la persona es femenina o masculina.")
    @NotBlank(message = "Indica el género de la persona.") String genero,
    @NotBlank(message = "Elige el perfil de la persona.")
    @Pattern(regexp = "P-0[1-5]", message = "El perfil tiene que ser P-01 a P-05.") String rol
) { }
