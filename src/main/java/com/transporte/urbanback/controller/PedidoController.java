package com.transporte.urbanback.controller;

import com.transporte.urbanback.dto.PedidoAuditDTO;
import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "API para la gestión de pedidos de transporte")
public class PedidoController {

    private final PedidoService pedidoService;
    private final SecurityUtils securityUtils;

    @Autowired
    public PedidoController(PedidoService pedidoService, SecurityUtils securityUtils) {
        this.pedidoService = pedidoService;
        this.securityUtils = securityUtils;
    }

    private PedidoAuditDTO convertToDto(PedidoAudit audit) {
        PedidoAuditDTO dto = new PedidoAuditDTO();
        dto.setId(audit.getId());
        dto.setPedidoId(audit.getPedido() != null ? audit.getPedido().getId() : null);
        dto.setTipoOperacion(audit.getTipoOperacion().name());
        dto.setFechaCambio(audit.getFechaCambio());
        dto.setUsuarioEditor(audit.getUsuarioEditor() != null ? audit.getUsuarioEditor().getUsername() : "Desconocido");
        dto.setDetallesCambio(audit.getDetallesCambio());
        return dto;
    }

    @Operation(summary = "Crea un nuevo pedido",
               description = "Permite registrar un nuevo pedido en el sistema, asociándolo a un cliente y opcionalmente a un conductor y vehículo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o cliente/conductor/vehículo no encontrados",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Cliente, conductor o vehículo no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENTE')")
    public ResponseEntity<Pedido> crearPedido(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Detalles del pedido a crear. Se requiere 'cliente.id', pero 'conductor' y 'vehiculo' son opcionales.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Pedido.class))
            ) Pedido pedido) {
        // Las excepciones ResourceNotFoundException y IllegalArgumentException
        // serán capturadas por el GlobalExceptionHandler
        Pedido nuevoPedido = pedidoService.crearPedido(pedido, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtiene un pedido por su ID",
               description = "Retorna los detalles de un pedido específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or " +
                "hasRole('CLIENTE') and @pedidoService.esPedidoPertenecienteACliente(#id, principal.username) or " +
                "hasRole('CONDUCTOR') and @pedidoService.esPedidoAsignadoAConductor(#id, principal.username)")
    public ResponseEntity<Pedido> obtenerPedidoPorId(
            @Parameter(description = "ID del pedido a buscar", example = "1") @PathVariable Long id) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return pedidoService.obtenerPedidoPorId(id)
                .map(pedido -> new ResponseEntity<>(pedido, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));
    }

    @Operation(summary = "Obtiene todos los pedidos",
               description = "Retorna una lista de todos los pedidos registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pedido>> obtenerTodosLosPedidos() {
        List<Pedido> pedidos = pedidoService.obtenerTodosLosPedidos();
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @GetMapping("/todos/paginado")
    @Operation(summary = "Obtener todos los pedidos con paginación",
               description = "Retorna una lista paginada de todos los pedidos registrados. " +
                             "Se pueden usar parámetros como 'page', 'size' y 'sort'.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos obtenida exitosamente")
               })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Pedido>> obtenerTodosLosPedidosPaginados(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Pedido> pedidos = pedidoService.obtenerTodosLosPedidosPaginados(pageable);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza un pedido existente",
               description = "Modifica los detalles de un pedido específico por su ID. Algunos campos como el estado deben ser actualizados con métodos específicos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o dependencias no encontradas",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pedido> actualizarPedido(
            @Parameter(description = "ID del pedido a actualizar", example = "1") @PathVariable Long id,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Detalles actualizados del pedido. Los campos nulos no se modificarán, excepto si se desea desasignar conductor/vehiculo.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Pedido.class))
            ) Pedido pedidoActualizado) {
        // Las excepciones EntityNotFoundException y IllegalArgumentException
        // serán capturadas por el GlobalExceptionHandler
        Pedido updatedPedido = pedidoService.actualizarPedido(id, pedidoActualizado, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(updatedPedido, HttpStatus.OK);
    }

    @Operation(summary = "Elimina un pedido",
               description = "Elimina un pedido del sistema por su ID. Si el pedido tiene dependencias, la eliminación podría fallar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de integridad de datos (ej. el pedido tiene relaciones)"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPedido(
            @Parameter(description = "ID del pedido a eliminar", example = "1") @PathVariable Long id) {
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        pedidoService.eliminarPedido(id, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Asigna un conductor y vehículo a un pedido",
               description = "Asigna o reasigna un conductor y un vehículo a un pedido existente, actualizando su estado si es necesario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conductor y vehículo asignados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. conductor/vehículo inactivo, ya asignado, límites alcanzados)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Pedido, conductor o vehículo no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/asignar/{pedidoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pedido> asignarConductorYVehiculo(
            @Parameter(description = "ID del pedido a asignar", example = "1") @PathVariable Long pedidoId,
            @Parameter(description = "ID del conductor a asignar", example = "10") @RequestParam Long conductorId,
            @Parameter(description = "ID del vehículo a asignar", example = "20") @RequestParam Long vehiculoId) {
        // Las excepciones EntityNotFoundException, IllegalArgumentException, IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Pedido updatedPedido = pedidoService.asignarConductorYVehiculo(pedidoId, conductorId, vehiculoId, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(updatedPedido, HttpStatus.OK);
    }

    @Operation(summary = "Cambia el estado de un pedido",
               description = "Actualiza el estado de un pedido (ej. PENDIENTE, ASIGNADO, EN_CAMINO, ENTREGADO, CANCELADO).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del pedido actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido o transición no permitida",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/estado/{pedidoId}/{nuevoEstado}")
    @PreAuthorize("hasRole('ADMIN') or " +
                  "hasRole('CONDUCTOR') and @pedidoService.esPedidoAsignadoAConductor(#pedidoId, principal.username)")
    public ResponseEntity<Pedido> cambiarEstadoPedido(
            @Parameter(description = "ID del pedido a actualizar", example = "1") @PathVariable Long pedidoId,
            @Parameter(description = "Nuevo estado para el pedido", example = "EN_CAMINO", schema = @Schema(type = "string", allowableValues = {"PENDIENTE", "ASIGNADO", "EN_CAMINO", "ENTREGADO", "CANCELADO"}))
            @PathVariable EstadoPedido nuevoEstado) {
        // Las excepciones EntityNotFoundException y IllegalArgumentException
        // serán capturadas por el GlobalExceptionHandler
        Pedido updatedPedido = pedidoService.cambiarEstadoPedido(pedidoId, nuevoEstado, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(updatedPedido, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por ID de cliente",
               description = "Retorna todos los pedidos asociados a un cliente específico.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos del cliente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-cliente/{clienteId}")
     @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and  @securityUtils.isClienteOwnedByCurrentUser(#clienteId))")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorCliente(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long clienteId) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por ID de conductor",
               description = "Retorna todos los pedidos asignados a un conductor específico.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos del conductor",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-conductor/{conductorId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdLinkedToCurrentUser(#conductorId))")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorConductor(
            @Parameter(description = "ID del conductor", example = "10") @PathVariable Long conductorId) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorConductor(conductorId);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por estado",
               description = "Retorna una lista de pedidos que se encuentran en un estado particular (PENDIENTE, ASIGNADO, EN_CAMINO, ENTREGADO, CANCELADO).")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos por estado",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorEstado(
            @Parameter(description = "Estado del pedido", example = "PENDIENTE", schema = @Schema(type = "string", allowableValues = {"PENDIENTE", "ASIGNADO", "EN_CAMINO", "ENTREGADO", "CANCELADO"}))
            @PathVariable EstadoPedido estado) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorEstado(estado);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por rango de fechas de creación",
               description = "Retorna pedidos creados dentro de un rango de fechas y horas específico.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos en el rango de fechas",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-fecha-creacion")
     @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorRangoFechasCreacion(
            @Parameter(description = "Fecha y hora de inicio (formato ISO)", example = "2023-01-01T00:00:00") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @Parameter(description = "Fecha y hora de fin (formato ISO)", example = "2023-12-31T23:59:59") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorRangoFechasCreacion(fechaInicio, fechaFin);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene el historial de cambios de un pedido específico",
               description = "Retorna todos los registros de auditoría para un pedido dado por su ID.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios del pedido",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoAuditDTO.class))))
    @GetMapping("/auditoria/{pedidoId}")
     @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoAuditDTO>> obtenerHistorialCambiosPorPedido(
            @Parameter(description = "ID del pedido para obtener su historial de auditoría", example = "1") @PathVariable Long pedidoId) {
        List<PedidoAudit> audits = pedidoService.obtenerHistorialCambiosPorPedido(pedidoId);
        List<PedidoAuditDTO> dtos = audits.stream()
                                            .map(this::convertToDto)
                                            .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene el historial de cambios de pedidos por usuario editor",
               description = "Retorna todos los registros de auditoría de pedidos realizados por un usuario específico.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios por usuario",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoAuditDTO.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de cambios por usuario editor",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoAuditDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/auditoria/por-usuario/{usernameEditor}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoAuditDTO>> obtenerHistorialCambiosPorUsuarioEditor(
            @Parameter(description = "Nombre de usuario del editor", example = "admin_user") @PathVariable String usernameEditor) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        List<PedidoAudit> audits = pedidoService.obtenerHistorialCambiosPorUsuarioEditor(usernameEditor);
        List<PedidoAuditDTO> dtos = audits.stream()
                                            .map(this::convertToDto)
                                            .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por conductor y estado",
               description = "Retorna pedidos asignados a un conductor específico y que se encuentran en un estado particular.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-conductor/{conductorId}/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdLinkedToCurrentUser(#conductorId) )")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorConductorYEstado(
            @Parameter(description = "ID del conductor", example = "10") @PathVariable Long conductorId,
            @Parameter(description = "Estado del pedido", example = "EN_CAMINO", schema = @Schema(type = "string", allowableValues = {"PENDIENTE", "ASIGNADO", "EN_CAMINO", "ENTREGADO", "CANCELADO"}))
            @PathVariable EstadoPedido estado) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorConductorYEstado(conductorId, estado);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene pedidos por cliente y estado",
               description = "Retorna pedidos asociados a un cliente específico y que se encuentran en un estado particular.")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pedido.class))))
    @GetMapping("/por-cliente/{clienteId}/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isClienteOwnedByCurrentUser(#clienteId) )")
    public ResponseEntity<List<Pedido>> obtenerPedidosPorClienteYEstado(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long clienteId,
            @Parameter(description = "Estado del pedido", example = "ENTREGADO", schema = @Schema(type = "string", allowableValues = {"PENDIENTE", "ASIGNADO", "EN_CAMINO", "ENTREGADO", "CANCELADO"}))
            @PathVariable EstadoPedido estado) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorClienteYEstado(clienteId, estado);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @GetMapping("/estados-de-pedido")
    @Operation(summary = "Obtener todos los estados de pedido",
               description = "Permite obtener una lista de todos los posibles estados que un pedido puede tener en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de estados obtenida exitosamente")
               })
    public ResponseEntity<List<EstadoPedido>> getAllEstadosDePedido() {
        List<EstadoPedido> estados = pedidoService.obtenerTodosLosEstadosDePedido();
        return ResponseEntity.ok(estados);
    }
}
