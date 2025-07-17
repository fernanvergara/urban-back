package com.transporte.urbanback.service.impl;

import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.security.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para UsuarioServiceImpl")
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConductorRepository conductorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private RegisterRequest registerRequestAdmin;
    private RegisterRequest registerRequestConductor;
    private RegisterRequest registerRequestCliente;
    private Usuario usuarioAdmin;
    private Conductor conductor;

    @BeforeEach
    void setUp() {
        registerRequestAdmin = new RegisterRequest("adminuser", "pass123", Rol.ADMIN, null, null);
        registerRequestConductor = new RegisterRequest("driveruser", "pass123", Rol.CONDUCTOR, 1L, null);
        registerRequestCliente = new RegisterRequest("clientuser", "pass123", Rol.CLIENTE, null, 1L);

        usuarioAdmin = new Usuario(1L, "adminuser", "encodedPass", Rol.ADMIN, null, null, true);
        conductor = new Conductor(1L, "Juan Perez", "123456789", LocalDate.of(1980, 1, 1), "1234567890", true);
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario ADMIN exitosamente")
    void whenRegistrarNuevoUsuario_Admin_thenReturnUsuario() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioAdmin);

        Usuario nuevoUsuario = usuarioService.registrarNuevoUsuario(registerRequestAdmin);

        assertNotNull(nuevoUsuario);
        assertEquals("adminuser", nuevoUsuario.getUsername());
        assertEquals(Rol.ADMIN, nuevoUsuario.getRol());
        assertTrue(nuevoUsuario.getActivo());
        verify(usuarioRepository, times(1)).existsByUsername("adminuser");
        verify(passwordEncoder, times(1)).encode("pass123");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario CONDUCTOR exitosamente")
    void whenRegistrarNuevoUsuario_Conductor_thenReturnUsuario() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(conductorRepository.findById(anyLong())).thenReturn(Optional.of(conductor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L); // Simular ID generado
            return u;
        });

        Usuario nuevoUsuario = usuarioService.registrarNuevoUsuario(registerRequestConductor);

        assertNotNull(nuevoUsuario);
        assertEquals("driveruser", nuevoUsuario.getUsername());
        assertEquals(Rol.CONDUCTOR, nuevoUsuario.getRol());
        assertNotNull(nuevoUsuario.getConductor());
        assertEquals(conductor.getId(), nuevoUsuario.getConductor().getId());
        verify(usuarioRepository, times(1)).existsByUsername("driveruser");
        verify(passwordEncoder, times(1)).encode("pass123");
        verify(conductorRepository, times(1)).findById(1L);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre de usuario ya existe al registrar")
    void whenRegistrarNuevoUsuario_UsernameExists_thenThrowIllegalArgumentException() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrarNuevoUsuario(registerRequestAdmin));

        verify(usuarioRepository, times(1)).existsByUsername("adminuser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el rol es CONDUCTOR y conductorId es nulo")
    void whenRegistrarNuevoUsuario_ConductorRolAndNullConductorId_thenThrowIllegalArgumentException() {
        registerRequestConductor.setConductorId(null); // Establecer conductorId a null
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrarNuevoUsuario(registerRequestConductor));

        verify(usuarioRepository, times(1)).existsByUsername("driveruser");
        verify(passwordEncoder, times(1)).encode("pass123");
        verify(conductorRepository, never()).findById(anyLong());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el conductor no se encuentra al registrar usuario CONDUCTOR")
    void whenRegistrarNuevoUsuario_ConductorNotFound_thenThrowEntityNotFoundException() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(conductorRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                usuarioService.registrarNuevoUsuario(registerRequestConductor));

        verify(usuarioRepository, times(1)).existsByUsername("driveruser");
        verify(passwordEncoder, times(1)).encode("pass123");
        verify(conductorRepository, times(1)).findById(1L);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe buscar un usuario por nombre de usuario y encontrarlo")
    void whenBuscarPorNombreUsuario_Found_thenReturnOptionalUsuario() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(usuarioAdmin));

        Optional<Usuario> encontrado = usuarioService.buscarPorNombreUsuario("adminuser");

        assertTrue(encontrado.isPresent());
        assertEquals("adminuser", encontrado.get().getUsername());
        verify(usuarioRepository, times(1)).findByUsername("adminuser");
    }

    @Test
    @DisplayName("Debe buscar un usuario por nombre de usuario y no encontrarlo")
    void whenBuscarPorNombreUsuario_NotFound_thenReturnEmptyOptional() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        Optional<Usuario> encontrado = usuarioService.buscarPorNombreUsuario("nonexistent");

        assertFalse(encontrado.isPresent());
        verify(usuarioRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Debe crear y guardar un usuario ADMIN si no existe")
    void whenCrearYGuardarAdmin_AdminDoesNotExist_thenCreateAndReturnAdmin() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedAdminPass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L); // Simular ID generado
            return u;
        });

        Usuario adminCreado = usuarioService.crearYGuardarAdmin("newadmin", "adminpass");

        assertNotNull(adminCreado);
        assertEquals("newadmin", adminCreado.getUsername());
        assertEquals(Rol.ADMIN, adminCreado.getRol());
        assertTrue(adminCreado.getActivo());
        verify(usuarioRepository, times(1)).findByUsername("newadmin");
        verify(passwordEncoder, times(1)).encode("adminpass");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe devolver el usuario ADMIN existente si ya existe")
    void whenCrearYGuardarAdmin_AdminExists_thenReturnExistingAdmin() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(usuarioAdmin));

        Usuario adminExistente = usuarioService.crearYGuardarAdmin("adminuser", "ignoredpass");

        assertNotNull(adminExistente);
        assertEquals("adminuser", adminExistente.getUsername());
        verify(usuarioRepository, times(1)).findByUsername("adminuser");
        verify(passwordEncoder, never()).encode(anyString()); // No debe codificar si ya existe
        verify(usuarioRepository, never()).save(any(Usuario.class)); // No debe guardar si ya existe
    }

    @Test
    @DisplayName("Debe guardar un usuario existente o modificado")
    void whenGuardarUsuario_thenReturnSavedUsuario() {
        Usuario usuarioModificado = new Usuario(1L, "updateduser", "newencodedpass", Rol.CLIENTE, null, null, false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioModificado);

        Usuario resultado = usuarioService.guardarUsuario(usuarioModificado);

        assertNotNull(resultado);
        assertEquals("updateduser", resultado.getUsername());
        verify(usuarioRepository, times(1)).save(usuarioModificado);
    }
}
