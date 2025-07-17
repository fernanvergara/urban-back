package com.transporte.urbanback.config;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner inicializarDatos(UsuarioService usuarioService) { 
        return args -> {
            log.info("Inicializando datos de la aplicación...");

            final String nombreUsuarioAdmin = "admin";
            final String contrasenaAdmin = "admin"; 

            // Verificar si el usuario 'admin' ya existe
            if (usuarioService.buscarPorNombreUsuario(nombreUsuarioAdmin).isEmpty()) {
                Usuario adminCreado = usuarioService.crearYGuardarAdmin(nombreUsuarioAdmin, contrasenaAdmin);
                log.info("Usuario 'admin' creado exitosamente. ID: {}", adminCreado.getId());
            } else {
                log.info("Usuario 'admin' ya existe.");
            }

            log.info("Inicialización de datos completada.");
        };
    }
}