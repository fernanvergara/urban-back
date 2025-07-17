package com.transporte.urbanback.service;

import java.util.Optional;

import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.security.Usuario;

public interface UsuarioService {
    Usuario registrarNuevoUsuario(RegisterRequest request);
    Optional<Usuario> buscarPorNombreUsuario(String username);
    Usuario crearYGuardarAdmin(String username, String rawPassword); 
    Usuario guardarUsuario(Usuario usuario); // Método genérico para guardar usuarios
}
