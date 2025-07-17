package com.transporte.urbanback.utilidades;

import org.springframework.beans.factory.annotation.Autowired;

import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.security.Usuario;

public class Utilidades {

    @Autowired
    private static UsuarioRepository usuarioRepository;

    public static Usuario getUsuarioEditor(String usernameEditor) {
        return usuarioRepository.findByUsername(usernameEditor)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario editor no encontrado: " + usernameEditor)); 
    }

}
