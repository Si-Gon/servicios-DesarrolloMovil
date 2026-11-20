package com.rinconsur.gateway.seguridad;

import org.springframework.stereotype.Component;

// La política se escribe acá y no repartida por las rutas: así se puede leer como una tabla
// y sostener en la defensa. Son dos preguntas por petición.
@Component
public class PoliticaDeAcceso {

    private static final String ADMINISTRADOR = "P-05";

    // Lo que se puede pedir sin token: el inicio de sesión y las preguntas del primer acceso.
    // La documentación también queda abierta, para poder revisar la API sin credenciales.
    public boolean esPublica(String metodo, String ruta) {
        return "OPTIONS".equals(metodo)
            || ruta.startsWith("/api/auth/")
            || ruta.startsWith("/v3/api-docs")
            || ruta.startsWith("/swagger-ui")
            || ruta.startsWith("/docs/")
            || ruta.startsWith("/actuator/");
    }

    // Solo la administradora crea cuentas y ve la lista completa de personas.
    // Ojo con el par método/ruta: GET /api/usuarios es el listado, GET /api/usuarios/{id} es
    // "mi ficha" y la puede pedir cualquiera con sesión.
    public boolean esSoloAdministrador(String metodo, String ruta) {
        if (ruta.equals("/api/usuarios")) {
            return true;
        }
        if (ruta.startsWith("/api/usuarios/") && !"GET".equals(metodo)) {
            return true;
        }
        return "POST".equals(metodo) && ruta.equals("/api/usuarios");
    }

    public boolean esAdministrador(String rol) {
        return ADMINISTRADOR.equals(rol);
    }
}
