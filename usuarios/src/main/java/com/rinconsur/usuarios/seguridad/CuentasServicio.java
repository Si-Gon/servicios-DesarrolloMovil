package com.rinconsur.usuarios.seguridad;

import com.rinconsur.usuarios.dto.CrearUsuarioPeticion;
import com.rinconsur.usuarios.dto.UsuarioCreado;
import com.rinconsur.usuarios.dto.UsuarioVista;
import com.rinconsur.usuarios.excepcion.ErrorDeNegocio;
import com.rinconsur.usuarios.model.Rol;
import com.rinconsur.usuarios.model.Usuario;
import com.rinconsur.usuarios.repository.RolRepositorio;
import com.rinconsur.usuarios.repository.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Las reglas de las cuentas viven acá y no en el controlador: el controlador solo traduce
// HTTP, así la misma regla sirve para la app y para una prueba.
@Service
public class CuentasServicio {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final ClavesServicio claves;
    private final RutServicio ruts;

    public CuentasServicio(UsuarioRepositorio usuarios, RolRepositorio roles,
                           ClavesServicio claves, RutServicio ruts) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.claves = claves;
        this.ruts = ruts;
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String rut, String clave) {
        String normalizado = ruts.normalizar(rutValido(rut));
        Usuario u = usuarios.findByRut(normalizado)
            .orElseThrow(() -> ErrorDeNegocio.noAutorizado("El RUT o la clave no coinciden."));
        if (!claves.verificar(u.getRut(), clave, u.getPasswordHash())) {
            throw ErrorDeNegocio.noAutorizado("El RUT o la clave no coinciden.");
        }
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw ErrorDeNegocio.prohibido("La cuenta está desactivada.");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public List<UsuarioVista> listar() {
        return usuarios.findAllByOrderByRolIdAscNombresAsc().stream().map(UsuarioVista::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioVista buscar(Integer id) {
        return usuarios.findById(id).map(UsuarioVista::de)
            .orElseThrow(() -> ErrorDeNegocio.noEncontrado("No existe una cuenta con ese identificador."));
    }

    // El administrador crea la persona y recibe el código temporal para entregarlo en mano.
    @Transactional
    public UsuarioCreado crear(CrearUsuarioPeticion p) {
        String rut = ruts.normalizar(rutValido(p.rut()));
        if (usuarios.existsByRut(rut)) {
            throw ErrorDeNegocio.conflicto("Ya existe una cuenta con el RUT " + rut + ".");
        }
        if (usuarios.existsByCorreo(p.correo().trim())) {
            throw ErrorDeNegocio.conflicto("Ya existe una cuenta con ese correo.");
        }
        Rol rol = roles.findByCodigo(p.rol())
            .orElseThrow(() -> ErrorDeNegocio.invalido("El perfil indicado no existe."));

        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombres(p.nombres().trim());
        u.setApellidoPaterno(p.apellidoPaterno().trim());
        u.setApellidoMaterno(p.apellidoMaterno() == null ? null : p.apellidoMaterno().trim());
        u.setGenero(p.genero());
        u.setCargo(cargoDe(rol.getCodigo(), p.genero(), p.nombres().trim()));
        u.setCorreo(p.correo().trim());
        u.setTelefono(p.telefono());
        u.setRol(rol);
        u.setActivo(Boolean.TRUE);
        u.setDebeCambiarClave(Boolean.TRUE);

        String codigo = claves.codigoTemporal();
        u.setPasswordHash(claves.hashear(rut, codigo));
        u.setCodigoVence(LocalDateTime.now().plusDays(7));

        Usuario guardado = usuarios.save(u);
        return new UsuarioCreado(UsuarioVista.de(guardado), codigo);
    }

    // El cargo sale del perfil y del género: no se escribe a mano, así no hay dos cargos
    // distintos para la misma persona ni un "vendedor" en una cuenta de supervisor.
    public static String cargoDe(String codigoRol, String genero, String nombres) {
        boolean femenino = !"M".equals(genero);
        return switch (codigoRol) {
            case "P-01" -> femenino ? "Jefa de tienda" : "Jefe de tienda";
            case "P-02" -> femenino ? "Subgerenta comercial" : "Subgerente comercial";
            case "P-03" -> femenino ? "Supervisora de piso" : "Supervisor de piso";
            case "P-04" -> femenino ? "Colaboradora observadora" : "Colaborador observador";
            case "P-05" -> femenino ? "Administradora" : "Administrador";
            default -> throw ErrorDeNegocio.invalido("El perfil indicado no existe.");
        };
    }

    private String rutValido(String rut) {
        if (!ruts.esValido(rut)) {
            throw ErrorDeNegocio.invalido("El RUT no es válido.");
        }
        return rut;
    }
}
