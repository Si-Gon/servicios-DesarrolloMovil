package com.rinconsur.usuarios.controller;

import com.rinconsur.usuarios.dto.CrearUsuarioPeticion;
import com.rinconsur.usuarios.dto.LoginPeticion;
import com.rinconsur.usuarios.dto.LoginRespuesta;
import com.rinconsur.usuarios.dto.UsuarioCreado;
import com.rinconsur.usuarios.dto.UsuarioVista;
import com.rinconsur.usuarios.excepcion.ErrorDeNegocio;
import com.rinconsur.usuarios.excepcion.ManejadorDeErrores;
import com.rinconsur.usuarios.model.PreguntaSeguridad;
import com.rinconsur.usuarios.model.Rol;
import com.rinconsur.usuarios.repository.PreguntaSeguridadRepositorio;
import com.rinconsur.usuarios.repository.RolRepositorio;
import com.rinconsur.usuarios.seguridad.CuentasServicio;
import com.rinconsur.usuarios.seguridad.JwtServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Lo que ve la app: el código de estado y el texto de "motivo". Se arma el controlador solo, con
// los servicios simulados y el consejo de excepciones real, así se prueba el contrato completo
// sin levantar la base.
@DisplayName("API de cuentas y login")
class CuentasControllerTest {

    private CuentasServicio cuentas;
    private JwtServicio jwt;
    private PreguntaSeguridadRepositorio preguntas;
    private RolRepositorio roles;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        cuentas = mock(CuentasServicio.class);
        jwt = mock(JwtServicio.class);
        preguntas = mock(PreguntaSeguridadRepositorio.class);
        roles = mock(RolRepositorio.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new AuthController(cuentas, jwt, preguntas),
                new UsuariosController(cuentas, roles))
            .setControllerAdvice(new ManejadorDeErrores())
            .build();
    }

    // La cuenta completa: el controlador arma la vista a partir de ella y una entidad vacía
    // reventaría en el rol nulo.
    private com.rinconsur.usuarios.model.Usuario cuentaCompleta() {
        Rol rol = new Rol();
        rol.setId(1);
        rol.setCodigo("P-01");
        rol.setNombre("Jefa de tienda");
        com.rinconsur.usuarios.model.Usuario u = new com.rinconsur.usuarios.model.Usuario();
        u.setId(1);
        u.setRut("15.234.871-1");
        u.setNombres("Carolina");
        u.setApellidoPaterno("Soto");
        u.setGenero("F");
        u.setCargo("Jefa de tienda");
        u.setCorreo("carolina@rinconsur.cl");
        u.setRol(rol);
        u.setActivo(true);
        u.setDebeCambiarClave(false);
        return u;
    }

    private UsuarioVista vista() {
        return new UsuarioVista(1, "15.234.871-1", "Carolina Soto", "Jefa de tienda",
            "P-01", "carolina@rinconsur.cl", "+56912345678", "F", true, false);
    }

    @Test
    void login_conCredencialesCorrectas_devuelveElTokenYLaVista() throws Exception {
        when(cuentas.autenticar("15.234.871-1", "rincon2026")).thenReturn(cuentaCompleta());
        when(jwt.emitir(any())).thenReturn("token-de-prueba");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"15.234.871-1\",\"clave\":\"rincon2026\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token-de-prueba"));
    }

    @Test
    void login_conClaveEquivocada_responde401ConElMotivoEnCastellano() throws Exception {
        when(cuentas.autenticar(anyString(), anyString()))
            .thenThrow(ErrorDeNegocio.noAutorizado("El RUT o la clave no coinciden."));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"15.234.871-1\",\"clave\":\"mala\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.motivo").value("El RUT o la clave no coinciden."));
    }

    @Test
    void login_sinEscribirLaClave_responde400ConElMensajeDelFormulario() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"15.234.871-1\",\"clave\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value("Escribe tu clave."));
    }

    @Test
    void preguntas_devuelveLasPreguntasActivasParaElPrimerAcceso() throws Exception {
        PreguntaSeguridad p = new PreguntaSeguridad();
        p.setId(1);
        p.setTexto("¿Cuál es el nombre de tu primera mascota?");
        when(preguntas.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(p));

        mvc.perform(get("/api/auth/preguntas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].texto").value("¿Cuál es el nombre de tu primera mascota?"));
    }

    @Test
    void listar_devuelveLasCuentasConSuPerfil() throws Exception {
        when(cuentas.listar()).thenReturn(List.of(vista()));

        mvc.perform(get("/api/usuarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Carolina Soto"))
            .andExpect(jsonPath("$[0].rol").value("P-01"));
    }

    @Test
    void buscar_unaCuentaQueNoExiste_responde404ConMotivo() throws Exception {
        when(cuentas.buscar(99))
            .thenThrow(ErrorDeNegocio.noEncontrado("No existe una cuenta con ese identificador."));

        mvc.perform(get("/api/usuarios/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.motivo").value("No existe una cuenta con ese identificador."));
    }

    @Test
    void roles_devuelveLosCincoPerfilesDelCaso() throws Exception {
        Rol r = new Rol();
        r.setId(1);
        r.setCodigo("P-01");
        r.setNombre("Jefa de tienda");
        when(roles.findAllByOrderByIdAsc()).thenReturn(List.of(r));

        mvc.perform(get("/api/usuarios/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigo").value("P-01"));
    }

    @Test
    void crear_unaCuenta_devuelve201ConElCodigoTemporalUnaSolaVez() throws Exception {
        when(cuentas.crear(any(CrearUsuarioPeticion.class)))
            .thenReturn(new UsuarioCreado(vista(), "ABCD2345"));

        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rut":"15.234.871-1","nombres":"Carolina","apellidoPaterno":"Soto",
                     "correo":"carolina@rinconsur.cl","genero":"F","rol":"P-01"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigoTemporal").value("ABCD2345"))
            .andExpect(jsonPath("$.usuario.rut").value("15.234.871-1"));
    }

    @Test
    void crear_conUnCorreoMalEscrito_responde400ConElMensajeDelDto() throws Exception {
        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rut":"15.234.871-1","nombres":"Carolina","apellidoPaterno":"Soto",
                     "correo":"no-es-un-correo","genero":"F","rol":"P-01"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value("El correo no tiene un formato válido."));
    }

    @Test
    void crear_conUnPerfilFueraDeRango_responde400() throws Exception {
        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rut":"15.234.871-1","nombres":"Carolina","apellidoPaterno":"Soto",
                     "correo":"carolina@rinconsur.cl","genero":"F","rol":"P-09"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.motivo").value("El perfil tiene que ser P-01 a P-05."));
    }
}
