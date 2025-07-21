package com.transporte.urbanback.service;

import java.util.List;
import java.util.Optional;

import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.security.Usuario;

public interface UsuarioService {
    List<Usuario> obtenerTodosLosUsuarios();
    Optional<Usuario> obtenerUsuarioPorId(Long id);
    Usuario registrarNuevoUsuario(RegisterRequest request);
    Optional<Usuario> buscarPorNombreUsuario(String username);
    Usuario crearYGuardarAdmin(String username, String rawPassword); 
    Usuario guardarUsuario(Usuario usuario); // Método genérico para guardar usuarios
    Usuario cambiarEstadoUsuario(Long id, boolean nuevoEstado);
    List<Rol> obtenerTodosLosRoles();
}
