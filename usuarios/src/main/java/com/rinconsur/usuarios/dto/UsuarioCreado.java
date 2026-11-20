package com.rinconsur.usuarios.dto;

// El código temporal se devuelve una sola vez, para que el administrador lo entregue en mano.
public record UsuarioCreado(UsuarioVista usuario, String codigoTemporal) { }
