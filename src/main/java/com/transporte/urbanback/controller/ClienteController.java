package com.transporte.urbanback.controller;

import com.transporte.urbanback.dto.ClienteAuditDTO;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit;
import com.transporte.urbanback.service.ClienteService;
import com.transporte.urbanback.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException; 
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Gestión de perfiles de clientes y su auditoría")
public class ClienteController {

    private final ClienteService clienteService;
    private final SecurityUtils securityUtils;

    @Autowired
    public ClienteController(ClienteService clienteService, SecurityUtils securityUtils) {
        this.clienteService = clienteService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un nuevo cliente",
               description = "Solo accesible para administradores. Requiere los datos completos del cliente.",
               responses = {
                   @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos o identificación duplicada"),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)"),
                   @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado")
               })
    public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody Cliente cliente) {
        // Las excepciones IllegalArgumentException y EntityNotFoundException (ahora ResourceNotFoundException)
        // serán capturadas por el GlobalExceptionHandler
        Cliente nuevoCliente = clienteService.crearCliente(cliente, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and @securityUtils.isClienteOwnedByCurrentUser(#id))")
    @Operation(summary = "Obtiene un cliente por su ID",
               description = "Accesible para administradores (cualquier cliente) y para clientes (solo su propio perfil).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                   @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
               })
    public ResponseEntity<Cliente> obtenerClientePorId(@Parameter(description = "ID del cliente") @PathVariable Long id) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return clienteService.obtenerClientePorId(id)
                .map(cliente -> new ResponseEntity<>(cliente, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene todos los clientes",
               description = "Solo accesible para administradores. Retorna una lista de todos los clientes.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    public ResponseEntity<List<Cliente>> obtenerTodosLosClientes() {
        List<Cliente> clientes = clienteService.obtenerTodosLosClientes();
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    
    @GetMapping("/todos/paginado")
    @Operation(summary = "Obtener todos los clientes con paginación",
               description = "Retorna una lista paginada de todos los clientes registrados. " +
                             "Se pueden usar parámetros como 'page', 'size' y 'sort'.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de clientes obtenida exitosamente")
               })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Cliente>> obtenerTodosLosClientesPaginados(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Cliente> clientes = clienteService.obtenerTodosLosClientesPaginados(pageable);
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and @securityUtils.isClienteOwnedByCurrentUser(#id))")
    @Operation(summary = "Actualiza un cliente existente",
               description = "Accesible para administradores (cualquier cliente) y para clientes (solo su propio perfil). No se permite cambiar el email si se mantiene como campo único, o se debe gestionar por el servicio de usuario.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos o identificación duplicada"),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado")
               })
    public ResponseEntity<Cliente> actualizarCliente(
            @Parameter(description = "ID del cliente a actualizar") @PathVariable Long id,
            @Valid @RequestBody Cliente clienteActualizado) {
        // Las excepciones EntityNotFoundException y IllegalArgumentException
        // serán capturadas por el GlobalExceptionHandler
        Cliente cliente = clienteService.actualizarCliente(id, clienteActualizado, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(cliente, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un cliente",
               description = "Solo accesible para administradores. Si el cliente tiene relaciones (ej. pedidos), la eliminación puede fallar por integridad referencial.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)"),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado"),
                   @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el cliente debido a registros relacionados")
               })
    public ResponseEntity<Void> eliminarCliente(@Parameter(description = "ID del cliente a eliminar") @PathVariable Long id) {
        // Las excepciones EntityNotFoundException, IllegalStateException y SecurityException
        // serán capturadas por el GlobalExceptionHandler
        clienteService.eliminarCliente(id, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambia el estado activo/inactivo de un cliente",
               description = "Solo accesible para administradores. Para eliminación lógica o reactivación.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Estado del cliente actualizado",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)"),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado")
               })
    public ResponseEntity<Cliente> cambiarEstadoActivoCliente(
            @Parameter(description = "ID del cliente") @PathVariable Long id,
            @Parameter(description = "Nuevo estado (true para activo, false para inactivo)", example = "true") @RequestParam Boolean nuevoEstado) {
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Cliente cliente = clienteService.cambiarEstadoActivoCliente(id, nuevoEstado, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(cliente, HttpStatus.OK);
    }

    @GetMapping("/activos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene clientes activos",
               description = "Solo accesible para administradores. Retorna una lista de clientes con estado 'activo' en true.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes activos",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    public ResponseEntity<List<Cliente>> obtenerClientesActivos() {
        List<Cliente> clientes = clienteService.obtenerClientesActivos();
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    @GetMapping("/inactivos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene clientes inactivos",
               description = "Solo accesible para administradores. Retorna una lista de clientes con estado 'activo' en false.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes inactivos",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    public ResponseEntity<List<Cliente>> obtenerClientesInactivos() {
        List<Cliente> clientes = clienteService.obtenerClientesInactivos();
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and @securityUtils.isClienteOwnedByCurrentUser(#id))")
    @Operation(summary = "Obtiene el historial de auditoría de un cliente por ID",
               description = "Accesible para administradores (cualquier cliente) y para clientes (solo su propio historial).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de auditoría obtenido",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteAudit.class))),
                   @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                   @ApiResponse(responseCode = "404", description = "Cliente no encontrado para el historial")
               })
    public ResponseEntity<List<ClienteAuditDTO>> obtenerHistorialCambiosPorCliente(@Parameter(description = "ID del cliente") @PathVariable Long id) {
        // Aunque el servicio no lanza EntityNotFoundException para este método,
        // el PreAuthorize ya valida si el cliente existe y es accesible.
        List<ClienteAudit> auditorias = clienteService.obtenerHistorialCambiosPorCliente(id);
        return new ResponseEntity<>(mapeoLista(auditorias), HttpStatus.OK);
    }

    @GetMapping("/auditoria/identificacion/{identificacion}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene el historial de auditoría de un cliente por identificación",
               description = "Solo accesible para administradores. Retorna el historial de cambios de un cliente específico por su identificación.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de auditoría obtenido",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteAudit.class)))
               })
    public ResponseEntity<List<ClienteAuditDTO>> obtenerHistorialCambiosPorIdentificacion(
            @Parameter(description = "Identificación del cliente") @PathVariable String identificacion) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler si el cliente no existe
        List<ClienteAudit> auditorias = clienteService.obtenerHistorialCambiosPorIdentificacion(identificacion);
        return new ResponseEntity<>(mapeoLista(auditorias), HttpStatus.OK);
    }

    private List<ClienteAuditDTO> mapeoLista(List<ClienteAudit> lista){
        return lista.stream()
                    .map(audit -> {
                        ClienteAuditDTO dto = new ClienteAuditDTO();
                        dto.setId(audit.getId());
                        dto.setClienteId(audit.getCliente().getId());
                        dto.setDetallesCambio(audit.getDetallesCambio());
                        dto.setTipoOperacion(audit.getTipoOperacion().name());
                        dto.setFechaCambio(audit.getFechaCambio());
                        dto.setUsuarioEditor(audit.getUsuarioEditor() == null ? null : audit.getUsuarioEditor().getUsername());
                        return dto;
                    })
                    .collect(Collectors.toList());
    }

}
