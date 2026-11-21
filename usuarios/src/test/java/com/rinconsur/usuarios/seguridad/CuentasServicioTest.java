package com.rinconsur.usuarios.seguridad;

import com.rinconsur.usuarios.dto.CrearUsuarioPeticion;
import com.rinconsur.usuarios.dto.UsuarioCreado;
import com.rinconsur.usuarios.dto.UsuarioVista;
import com.rinconsur.usuarios.excepcion.ErrorDeNegocio;
import com.rinconsur.usuarios.model.Rol;
import com.rinconsur.usuarios.model.Usuario;
import com.rinconsur.usuarios.repository.RolRepositorio;
import com.rinconsur.usuarios.repository.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Las reglas de las cuentas: quién entra, quién no, y qué se guarda cuando el administrador
// crea a alguien. Se prueban sobre el servicio, que es donde viven, con el repositorio simulado.
@DisplayName("Cuentas de la tienda")
class CuentasServicioTest {

    private UsuarioRepositorio usuarios;
    private RolRepositorio roles;
    private CuentasServicio servicio;
    private ClavesServicio claves;
    private RutServicio ruts;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositorio.class);
        roles = mock(RolRepositorio.class);
        claves = new ClavesServicio();
        ruts = new RutServicio();
        servicio = new CuentasServicio(usuarios, roles, claves, ruts);
    }

    private Usuario cuenta(String rol, boolean activo, String clave) {
        Rol r = new Rol();
        r.setId(1);
        r.setCodigo(rol);
        r.setNombre("Jefa de tienda");
        Usuario u = new Usuario();
        u.setId(1);
        u.setRut("15.234.871-1");
        u.setNombres("Carolina");
        u.setApellidoPaterno("Soto");
        u.setGenero("F");
        u.setCargo("Jefa de tienda");
        u.setCorreo("carolina@rinconsur.cl");
        u.setRol(r);
        u.setActivo(activo);
        u.setPasswordHash(claves.hashear("15.234.871-1", clave));
        return u;
    }

    @Test
    void autenticar_conRutYClaveCorrectos_devuelveLaCuenta() {
        when(usuarios.findByRut("15.234.871-1"))
            .thenReturn(Optional.of(cuenta("P-01", true, "rincon2026")));

        Usuario u = servicio.autenticar("15234871-1", "rincon2026");

        assertThat(u.getId()).isEqualTo(1);
        assertThat(u.getRut()).isEqualTo("15.234.871-1");
    }

    @Test
    void autenticar_conUnRutQueNoExiste_noDiceQueElRutExiste() {
        when(usuarios.findByRut(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.autenticar("15.234.871-1", "rincon2026"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El RUT o la clave no coinciden.");
    }

    @Test
    void autenticar_conLaClaveDeOtraPersona_noDejaEntrar() {
        when(usuarios.findByRut("15.234.871-1"))
            .thenReturn(Optional.of(cuenta("P-01", true, "rincon2026")));

        assertThatThrownBy(() -> servicio.autenticar("15.234.871-1", "claveQueNoEs"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("no coinciden");
    }

    @Test
    void autenticar_conLaCuentaDesactivada_loDiceSinPedirLaClaveDeNuevo() {
        when(usuarios.findByRut("15.234.871-1"))
            .thenReturn(Optional.of(cuenta("P-01", false, "rincon2026")));

        assertThatThrownBy(() -> servicio.autenticar("15.234.871-1", "rincon2026"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("La cuenta está desactivada.");
    }

    @Test
    void autenticar_conUnRutInventado_loRechazaAntesDeConsultarLaBase() {
        assertThatThrownBy(() -> servicio.autenticar("15.234.871-2", "rincon2026"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El RUT no es válido.");

        verify(usuarios, never()).findByRut(anyString());
    }

    @Test
    void buscar_unaCuentaQueNoExiste_da404ConMotivo() {
        when(usuarios.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscar(99))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("No existe una cuenta");
    }

    @Test
    void listar_devuelveLasCuentasComoVistaSinElHashDeLaClave() {
        when(usuarios.findAllByOrderByRolIdAscNombresAsc())
            .thenReturn(List.of(cuenta("P-01", true, "rincon2026")));

        List<UsuarioVista> lista = servicio.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nombre()).isEqualTo("Carolina Soto");
        assertThat(lista.get(0).rol()).isEqualTo("P-01");
        assertThat(lista.get(0).correo()).isEqualTo("carolina@rinconsur.cl");
    }

    private CrearUsuarioPeticion peticion(String genero, String rol) {
        return new CrearUsuarioPeticion("17.654.320-5", "Daniela", "Rojas", "Pérez",
            "daniela@rinconsur.cl", "+56912345678", genero, rol);
    }

    @Test
    void crear_unaPersonaNueva_guardaElCargoDelPerfilYDevuelveCodigoTemporal() {
        Rol supervisor = new Rol();
        supervisor.setId(3);
        supervisor.setCodigo("P-03");
        supervisor.setNombre("Supervisora de piso");
        when(usuarios.existsByRut(anyString())).thenReturn(false);
        when(usuarios.existsByCorreo(anyString())).thenReturn(false);
        when(roles.findByCodigo("P-03")).thenReturn(Optional.of(supervisor));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioCreado creado = servicio.crear(peticion("F", "P-03"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        Usuario u = guardado.getValue();

        assertThat(u.getCargo()).isEqualTo("Supervisora de piso");
        assertThat(u.getActivo()).isTrue();
        assertThat(u.getDebeCambiarClave()).as("entra con un código, no con una clave elegida").isTrue();
        assertThat(u.getCodigoVence()).isNotNull();
        assertThat(u.getPasswordHash()).startsWith("pbkdf2$");
        assertThat(creado.codigoTemporal()).hasSize(8);
        assertThat(creado.usuario().rut()).isEqualTo("17.654.320-5");
    }

    @Test
    void crear_unHombre_enElMismoPerfil_guardaElCargoEnMasculino() {
        Rol supervisor = new Rol();
        supervisor.setCodigo("P-03");
        supervisor.setNombre("Supervisor de piso");
        when(usuarios.existsByRut(anyString())).thenReturn(false);
        when(usuarios.existsByCorreo(anyString())).thenReturn(false);
        when(roles.findByCodigo("P-03")).thenReturn(Optional.of(supervisor));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        servicio.crear(peticion("M", "P-03"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getCargo()).isEqualTo("Supervisor de piso");
    }

    @Test
    void crear_conUnRutQueYaExiste_noGuardaNada() {
        when(usuarios.existsByRut("17.654.320-5")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(peticion("F", "P-03")))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Ya existe una cuenta con el RUT");

        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void crear_conUnCorreoQueYaExiste_noGuardaNada() {
        when(usuarios.existsByRut(anyString())).thenReturn(false);
        when(usuarios.existsByCorreo("daniela@rinconsur.cl")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(peticion("F", "P-03")))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("Ya existe una cuenta con ese correo.");

        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void crear_conUnPerfilQueNoExiste_noGuardaNada() {
        when(usuarios.existsByRut(anyString())).thenReturn(false);
        when(usuarios.existsByCorreo(anyString())).thenReturn(false);
        when(roles.findByCodigo("P-09")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(peticion("F", "P-09")))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El perfil indicado no existe.");
    }

    @Test
    void crear_conUnRutInventado_loRechazaAntesDeMirarSiExiste() {
        CrearUsuarioPeticion mala = new CrearUsuarioPeticion("17.654.320-9", "Daniela", "Rojas", null,
            "daniela@rinconsur.cl", null, "F", "P-03");

        assertThatThrownBy(() -> servicio.crear(mala))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El RUT no es válido.");
    }

    @Test
    void cargoDe_cadaPerfil_tieneSuFormaFemeninaYMasculina() {
        assertThat(CuentasServicio.cargoDe("P-01", "F", "Ana")).isEqualTo("Jefa de tienda");
        assertThat(CuentasServicio.cargoDe("P-01", "M", "Ana")).isEqualTo("Jefe de tienda");
        assertThat(CuentasServicio.cargoDe("P-02", "F", "Ana")).isEqualTo("Subgerenta comercial");
        assertThat(CuentasServicio.cargoDe("P-02", "M", "Ana")).isEqualTo("Subgerente comercial");
        assertThat(CuentasServicio.cargoDe("P-04", "F", "Ana")).isEqualTo("Colaboradora observadora");
        assertThat(CuentasServicio.cargoDe("P-04", "M", "Ana")).isEqualTo("Colaborador observador");
        assertThat(CuentasServicio.cargoDe("P-05", "F", "Ana")).isEqualTo("Administradora");
        assertThat(CuentasServicio.cargoDe("P-05", "M", "Ana")).isEqualTo("Administrador");
    }

    @Test
    void cargoDe_sinGeneroDeclarado_caeEnLaFormaFemenina() {
        // El género es obligatorio al crear; si faltara, se prefiere el femenino que inventar un cargo.
        assertThat(CuentasServicio.cargoDe("P-03", null, "Ana")).isEqualTo("Supervisora de piso");
    }

    @Test
    void cargoDe_conUnCodigoQueNoExiste_loRechaza() {
        assertThatThrownBy(() -> CuentasServicio.cargoDe("P-09", "F", "Ana"))
            .isInstanceOf(ErrorDeNegocio.class)
            .hasMessageContaining("El perfil indicado no existe.");
    }
}
