package com.rinconsur.usuarios;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Frontera de seguridad del sistema: es el único servicio que toca credenciales y datos
// personales. Emite el token con el rol dentro, así los demás servicios no lo llaman para autorizar.
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
    title = "Rincón Sur - API de Usuarios",
    version = "1.0",
    description = "Cuentas de la tienda, roles, inicio de sesión y preguntas de seguridad"
))
public class UsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsuariosApplication.class, args);
    }
}
