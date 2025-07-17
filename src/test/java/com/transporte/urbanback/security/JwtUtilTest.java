package com.transporte.urbanback.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils; 

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Collections; 

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para JwtUtil.
 * Utiliza Mockito para simular UserDetails y ReflectionTestUtils para inyectar
 * los valores de las propiedades @Value.
 */
@ExtendWith(MockitoExtension.class) 
@DisplayName("Tests para JwtUtil")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    @Mock 
    private UserDetails userDetails;

    private final String TEST_SECRET = "supersecretkeythatisverylongandsecureforjwttokengeneration1234567"; 
    private final long TEST_EXPIRATION_MS = 3600000; 

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION_MS);
    }

    /**
     * Test para la generación de un token JWT.
     * Verifica que el token generado no sea nulo, no esté vacío
     * y que el nombre de usuario extraído sea correcto.
     */
    @Test
    @DisplayName("Debe generar un token JWT válido")
    void whenGenerateToken_thenTokenIsValid() {
        when(userDetails.getUsername()).thenReturn("testuser");
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        doReturn(authorities).when(userDetails).getAuthorities();

        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("testuser", extractedUsername);

        assertFalse(jwtUtil.isTokenExpired(token));

        Claims claims = jwtUtil.extractAllClaims(token);
        List<String> roles = (List<String>) claims.get("roles");
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    /**
     * Test para la extracción del nombre de usuario de un token.
     */
    @Test
    @DisplayName("Debe extraer el nombre de usuario correctamente")
    void whenExtractUsername_thenReturnsCorrectUsername() {
        when(userDetails.getUsername()).thenReturn("anotheruser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); 

        String token = jwtUtil.generateToken(userDetails);
        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals("anotheruser", extractedUsername);
    }

    /**
     * Test para la validación de un token válido.
     */
    @Test
    @DisplayName("Debe validar un token válido")
    void whenValidateToken_thenReturnsTrueForValidToken() {
        when(userDetails.getUsername()).thenReturn("validuser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); // Importado java.util.Collections

        String token = jwtUtil.generateToken(userDetails);
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    /**
     * Test para la validación de un token con nombre de usuario incorrecto.
     */
    @Test
    @DisplayName("Debe invalidar un token con nombre de usuario incorrecto")
    void whenValidateToken_thenReturnsFalseForIncorrectUsername() {
        when(userDetails.getUsername()).thenReturn("correctuser");

        String token = jwtUtil.generateToken(userDetails);

        UserDetails incorrectUserDetails = User.withUsername("incorrectuser")
                .password("pass")
                .authorities("ROLE_USER")
                .build();

        Boolean isValid = jwtUtil.validateToken(token, incorrectUserDetails);

        assertFalse(isValid);
    }

    /**
     * Test para la validación de un token expirado.
     * Se simula un token expirado ajustando la fecha de emisión.
     */
    @Test
    @DisplayName("Debe invalidar un token expirado")
    void whenValidateToken_thenReturnsFalseForExpiredToken() {
        when(userDetails.getUsername()).thenReturn("expireduser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); 

        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1L); 

        String token = jwtUtil.generateToken(userDetails);

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertFalse(isValid);
    }

    /**
     * Test para la extracción de la fecha de expiración.
     */
    @Test
    @DisplayName("Debe extraer la fecha de expiración correctamente")
    void whenExtractExpiration_thenReturnsCorrectDate() {
        when(userDetails.getUsername()).thenReturn("expuser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); 

        String token = jwtUtil.generateToken(userDetails);
        Date expirationDate = jwtUtil.extractExpiration(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date())); 
    }

    /**
     * Test para verificar si el token está expirado.
     */
    @Test
    @DisplayName("Debe indicar que un token está expirado")
    void whenIsTokenExpired_thenReturnsTrueForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1L); 

        when(userDetails.getUsername()).thenReturn("expiringuser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); 

        String token = jwtUtil.generateToken(userDetails);

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(jwtUtil.isTokenExpired(token));
    }

    /**
     * Test para verificar que un token no expirado se reporte como tal.
     */
    @Test
    @DisplayName("Debe indicar que un token no está expirado")
    void whenIsTokenExpired_thenReturnsFalseForNonExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION_MS);

        when(userDetails.getUsername()).thenReturn("nonexpiringuser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList()); 

        String token = jwtUtil.generateToken(userDetails);

        assertFalse(jwtUtil.isTokenExpired(token));
    }
}
