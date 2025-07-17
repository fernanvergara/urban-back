package com.transporte.urbanback.security;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para UserDetailsServiceImpl.
 * Utiliza Mockito para simular las dependencias.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Usuario testUsuario;

    @BeforeEach
    void setUp() {
        testUsuario = new Usuario(1L, "testuser", "encodedPassword", Rol.CLIENTE, null, null, true);
    }

    /**
     * Test para verificar loadUserByUsername cuando el usuario es encontrado.
     * Debe retornar un objeto UserDetails válido.
     */
    @Test
    @DisplayName("loadUserByUsername debe retornar UserDetails cuando el usuario es encontrado")
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.of(testUsuario));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals(testUsuario.getUsername(), userDetails.getUsername());
        assertEquals(testUsuario.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + testUsuario.getRol().name())));
        verify(usuarioRepository, times(1)).findByUsername("testuser");
    }

    /**
     * Test para verificar loadUserByUsername cuando el usuario no es encontrado.
     * Debe lanzar una UsernameNotFoundException.
     */
    @Test
    @DisplayName("loadUserByUsername debe lanzar UsernameNotFoundException cuando el usuario no es encontrado")
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("nonexistentuser"));

        assertTrue(thrown.getMessage().contains("Usuario no encontrado: nonexistentuser"));
        verify(usuarioRepository, times(1)).findByUsername("nonexistentuser");
    }
}
