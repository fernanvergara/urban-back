package com.transporte.urbanback.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Clase de pruebas para JwtRequestFilter.
 * Utiliza Mockito para simular las dependencias y Mockito.mockStatic para
 * probar interacciones con SecurityContextHolder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para JwtRequestFilter")
class JwtRequestFilterTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private UserDetails userDetails;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        userDetails = new User("testuser", "password", Collections.emptyList());

        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        securityContext = mock(SecurityContext.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    /**
     * Test para el escenario donde no hay encabezado de autorización.
     * El filtro debe simplemente continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si no hay encabezado de autorización")
    void whenNoAuthorizationHeader_thenFilterChainContinues() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario donde el encabezado de autorización no es "Bearer ".
     * El filtro debe simplemente continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si el encabezado de autorización no es 'Bearer '")
    void whenInvalidAuthorizationHeader_thenFilterChainContinues() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic some_token");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario de un token JWT válido y usuario no autenticado.
     * El filtro debe autenticar al usuario y continuar la cadena.
     */
    @Test
    @DisplayName("Debe autenticar un token JWT válido cuando el usuario no está autenticado")
    void whenValidJwtAndNotAuthenticated_thenAuthenticatesAndContinues() throws ServletException, IOException {
        String jwtToken = "valid.jwt.token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(jwtUtil.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(jwtToken, userDetails)).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).extractUsername(jwtToken);
        verify(userDetailsService, times(1)).loadUserByUsername(username);
        verify(jwtUtil, times(1)).validateToken(jwtToken, userDetails);
        verify(securityContext, times(1)).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario de un token JWT válido pero el usuario ya está autenticado.
     * El filtro no debe intentar autenticar de nuevo y debe continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si el usuario ya está autenticado")
    void whenValidJwtAndAlreadyAuthenticated_thenFilterChainContinuesWithoutReauthentication() throws ServletException, IOException {
        String jwtToken = "valid.jwt.token";
        String username = "testuser";
        Authentication existingAuth = mock(Authentication.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(jwtUtil.extractUsername(jwtToken)).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).extractUsername(jwtToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario de un token JWT inválido (validateToken devuelve false).
     * El filtro no debe autenticar y debe continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si el token JWT es inválido")
    void whenInvalidJwt_thenNoAuthenticationAndFilterChainContinues() throws ServletException, IOException {
        String jwtToken = "invalid.jwt.token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(jwtUtil.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(jwtToken, userDetails)).thenReturn(false);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).extractUsername(jwtToken);
        verify(userDetailsService, times(1)).loadUserByUsername(username);
        verify(jwtUtil, times(1)).validateToken(jwtToken, userDetails);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario donde extractUsername lanza ExpiredJwtException.
     * El filtro no debe autenticar y debe continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si el token está expirado (ExpiredJwtException)")
    void whenJwtExpired_thenNoAuthenticationAndFilterChainContinues() throws ServletException, IOException {
        String jwtToken = "expired.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(jwtUtil.extractUsername(jwtToken)).thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).extractUsername(jwtToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * Test para el escenario donde extractUsername lanza SignatureException (token corrupto/modificado).
     * El filtro no debe autenticar y debe continuar la cadena.
     */
    @Test
    @DisplayName("No debe autenticar si el token tiene firma inválida (SignatureException)")
    void whenJwtSignatureInvalid_thenNoAuthenticationAndFilterChainContinues() throws ServletException, IOException {
        String jwtToken = "invalid.signature.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(jwtUtil.extractUsername(jwtToken)).thenThrow(new SignatureException("Invalid JWT signature"));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).extractUsername(jwtToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
