package com.transporte.urbanback.utilidades;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.security.Usuario;

@Component
public class Utilidades {

    private static UsuarioRepository usuarioRepository;

    @Autowired
    public void setUsuarioRepository(UsuarioRepository usuarioRepository) {
        Utilidades.usuarioRepository = usuarioRepository;
    }

    public static Usuario getUsuarioEditor(String usernameEditor) {
        return usuarioRepository.findByUsername(usernameEditor)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario editor no encontrado: " + usernameEditor)); 
    }

}
