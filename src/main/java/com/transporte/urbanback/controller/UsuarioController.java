package com.transporte.urbanback.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Tag(name = "UsuarioController", description = "API para la gestión de Usuarios")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    private final SecurityUtils securityUtils;

    @Autowired
    public UsuarioController(UsuarioService usuarioService, SecurityUtils securityUtils) {
        this.usuarioService = usuarioService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/todos")
    @Operation(summary = "Obtener todos los usuarios", description = "Retorna una lista de todos los usuarios registrados.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> obtenerTodosLosUsuarios() {
        List<Usuario> usuarios = usuarioService.obtenerTodosLosUsuarios();
        return new ResponseEntity<>(usuarios, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna un usuario específico por su ID.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> obtenerUsuarioPorId(@PathVariable Long id) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return usuarioService.obtenerUsuarioPorId(id)
                .map(usuario -> new ResponseEntity<>(usuario, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Obtener usuario por username", description = "Retorna un usuario específico por su username.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> obtenerUsuarioPorUsername(@PathVariable String username) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return usuarioService.buscarPorNombreUsuario(username)
                .map(usuario -> new ResponseEntity<>(usuario, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con username: " + username));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario", description = "Crea un nuevo usuario en la base de datos.")
    @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos")
    @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> crearUsuario(
            @Valid @RequestBody RegisterRequest usuario) {
            // Las excepciones IllegalArgumentException y EntityNotFoundException
            // serán capturadas por el GlobalExceptionHandler
            Usuario nuevoUsuario = usuarioService.registrarNuevoUsuario(usuario);
            return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario existente", description = "Actualiza los datos de un usuario existente por su ID.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (ej. no ADMIN intentando cambiar identificación)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody Usuario usuarioDetalles) {
            // Las excepciones EntityNotFoundException, IllegalArgumentException y SecurityException
            // serán capturadas por el GlobalExceptionHandler
            Usuario usuarioActualizado = usuarioService.guardarUsuario( usuarioDetalles);
            return new ResponseEntity<>(usuarioActualizado, HttpStatus.OK);
    }

    @PatchMapping("/{id}/estado") // Usar PATCH para actualizar un campo parcial
    @Operation(summary = "Cambiar el estado activo de un usuario (eliminación lógica/activación)",
            description = "Permite inactivar (eliminar lógicamente) o reactivar un usuario por su ID.")
    @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "400", description = "Estado inválido proporcionado")
    @ApiResponse(responseCode = "409", description = "Conflicto: No se pudo actualizar el estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> cambiarEstadoActivoUsuario(
            @PathVariable Long id,
            @RequestParam Boolean activo) { // Recibir el nuevo estado (true/false)
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Usuario usuarioActualizado = usuarioService.cambiarEstadoUsuario(id, activo);
        return new ResponseEntity<>(usuarioActualizado, HttpStatus.OK);
    }

    @GetMapping("/roles")
    @Operation(summary = "Obtener todos los roles de usuario",
               description = "Permite obtener una lista de todos los posibles roles que un usuario puede ser.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
               })
    public ResponseEntity<List<Rol>> getAllEstadosDePedido() {
        List<Rol> estados = usuarioService.obtenerTodosLosRoles();
        return ResponseEntity.ok(estados);
    }

}
