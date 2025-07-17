package com.transporte.urbanback.config;

import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para DataInitializer.
 * Utiliza Mockito para simular UsuarioService y verificar las interacciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para DataInitializer")
class DataInitializerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private DataInitializer dataInitializer;

    private final String ADMIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";

    private Usuario adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new Usuario();
        adminUser.setId(1L);
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setPassword(ADMIN_PASSWORD); 
    }

    /**
     * Test para verificar que el usuario 'admin' se crea si no existe.
     */
    @Test
    @DisplayName("Debe crear el usuario 'admin' si no existe")
    void inicializarDatos_whenAdminDoesNotExist_thenCreateAdmin() throws Exception {
        when(usuarioService.buscarPorNombreUsuario(ADMIN_USERNAME)).thenReturn(Optional.empty());
        when(usuarioService.crearYGuardarAdmin(ADMIN_USERNAME, ADMIN_PASSWORD)).thenReturn(adminUser);

        CommandLineRunner runner = dataInitializer.inicializarDatos(usuarioService);
        runner.run();

        verify(usuarioService, times(1)).buscarPorNombreUsuario(ADMIN_USERNAME);
        verify(usuarioService, times(1)).crearYGuardarAdmin(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /**
     * Test para verificar que el usuario 'admin' no se crea si ya existe.
     */
    @Test
    @DisplayName("No debe crear el usuario 'admin' si ya existe")
    void inicializarDatos_whenAdminExists_thenDoNotCreateAdmin() throws Exception {
        when(usuarioService.buscarPorNombreUsuario(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        CommandLineRunner runner = dataInitializer.inicializarDatos(usuarioService);
        runner.run();

        verify(usuarioService, times(1)).buscarPorNombreUsuario(ADMIN_USERNAME);
        verify(usuarioService, never()).crearYGuardarAdmin(anyString(), anyString());
    }
}
