package com.transporte.urbanback.controller;

import com.transporte.urbanback.dto.JwtRequest;
import com.transporte.urbanback.dto.JwtResponse;
import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.security.JwtUtil;
import com.transporte.urbanback.security.UserDetailsServiceImpl;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AuthController", description = "API para Autenticación y Registro de Usuarios")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UsuarioService usuarioService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService, UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica un usuario y retorna un token JWT",
               description = "Requiere username y password para generar un token de autenticación.")
    @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "400", description = "Credenciales inválidas o cuenta deshabilitada")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<?> createAuthenticationToken(
            @Valid @RequestBody JwtRequest authenticationRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (DisabledException e) {
            return new ResponseEntity<>("Usuario deshabilitado.", HttpStatus.BAD_REQUEST);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>("Credenciales inválidas.", HttpStatus.BAD_REQUEST);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(jwt));
    }


    @PostMapping("/register")
    @Operation(summary = "Registra un nuevo usuario en el sistema",
               description = "Permite el registro de usuarios con diferentes roles. Si el rol es CONDUCTOR, se puede vincular a un Conductor existente.")
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "400", description = "Datos de registro inválidos, nombre de usuario ya en uso o ID de conductor no encontrado/ya vinculado")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado para vincular (si aplica)")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<Usuario> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Las excepciones IllegalArgumentException y ResourceNotFoundException
        // serán capturadas por el GlobalExceptionHandler
        Usuario newUser = usuarioService.registrarNuevoUsuario(registerRequest);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }
}
