package com.transporte.urbanback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.dto.JwtRequest;
import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.security.JwtUtil;
import com.transporte.urbanback.security.UserDetailsServiceImpl;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Clase de pruebas para AuthController.
 * Utiliza @WebMvcTest para enfocar el testeo en la capa web.
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("Tests para AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private UsuarioService usuarioService;

    private JwtRequest jwtRequest;
    private RegisterRequest registerRequest;
    private Usuario nuevoUsuario;
    private UserDetails userDetails;

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo para JWT y registro.
     */
    @BeforeEach
    void setUp() {
        jwtRequest = new JwtRequest("testuser", "password");
        registerRequest = new RegisterRequest("newuser", "newpass1", Rol.CLIENTE, null, null);
        nuevoUsuario = new Usuario(1L, "newuser", "encodedpass", Rol.CLIENTE, null, null, true);
        userDetails = new User("testuser", "password1", new ArrayList<>());
    }

    /**
     * Test para la autenticación exitosa.
     * Verifica que el endpoint POST /api/v1/auth/login retorne 200 OK con un token JWT.
     */
    @Test
    @DisplayName("Debe autenticar un usuario y retornar 200 OK con JWT")
    void whenCreateAuthenticationToken_thenReturn200OkWithJwt() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // No necesitamos un objeto Authentication real para este test
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("mocked_jwt_token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jwtRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("mocked_jwt_token"));
    }

    /**
     * Test para la autenticación con credenciales inválidas.
     * Verifica que el endpoint POST /api/v1/auth/login retorne 400 Bad Request.
     */
    @Test
    @DisplayName("Debe retornar 400 Bad Request para credenciales inválidas")
    void whenCreateAuthenticationToken_withInvalidCredentials_thenReturn400BadRequest() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jwtRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Credenciales inválidas."));
    }

    /**
     * Test para el registro de un nuevo usuario.
     * Verifica que el endpoint POST /api/v1/auth/register retorne 201 Created.
     */
    @Test
    @DisplayName("Debe registrar un nuevo usuario y retornar 201 Created")
    void whenRegisterUser_thenReturn201Created() throws Exception {
        when(usuarioService.registrarNuevoUsuario(any(RegisterRequest.class))).thenReturn(nuevoUsuario);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    /**
     * Test para el registro de un usuario con nombre de usuario ya en uso.
     * Verifica que el endpoint POST /api/v1/auth/register retorne 400 Bad Request.
     */
    @Test
    @DisplayName("Debe retornar 400 Bad Request si el nombre de usuario ya está en uso")
    void whenRegisterUser_usernameAlreadyInUse_thenReturn400BadRequest() throws Exception {
        String errorMessage = "El nombre de usuario 'newuser' ya esta en uso.";
        when(usuarioService.registrarNuevoUsuario(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException(errorMessage));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)) 
                .andExpect(jsonPath("$.error").value("Bad Request")) 
                .andExpect(jsonPath("$.message").value(errorMessage)) 
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register")); 
    }
}
