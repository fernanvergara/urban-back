package com.transporte.urbanback.service;

import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit; // Asumiendo que tendrás una entidad de auditoría para Pedido
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "PedidoService", description = "Servicio para la gestión de pedidos y su auditoría")
public interface PedidoService {

    @Operation(summary = "Crea un nuevo pedido y registra la auditoría",
               description = "Guarda un nuevo pedido en la base de datos y crea un registro de auditoría de tipo CREAR. El usuario asociado debe existir previamente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedido creado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de pedido inválidos o inconsistentes"),
                   @ApiResponse(responseCode = "404", description = "Usuario editor, cliente, conductor o vehículo no encontrado")
               })
    Pedido crearPedido(
            @Parameter(description = "Datos del pedido a crear", required = true) Pedido pedido,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Obtiene un pedido por su ID",
               description = "Busca y retorna un pedido específico por su identificador único.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedido encontrado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
                   @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
               })
    Optional<Pedido> obtenerPedidoPorId(
            @Parameter(description = "ID del pedido a buscar", example = "1") Long id
    );

    @Operation(summary = "Obtiene una lista de todos los pedidos",
               description = "Retorna una lista de todos los pedidos registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de pedidos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerTodosLosPedidos();

    @Operation(summary = "Obtiene una lista paginada de todos los pedidos",
               description = "Retorna una lista paginada de todos los pedidos registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    Page<Pedido> obtenerTodosLosPedidosPaginados(Pageable pageable);

    @Operation(summary = "Actualiza un pedido existente y registra la auditoría",
               description = "Actualiza los datos de un pedido existente. El pedido debe existir.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedido actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de pedido inválidos o inconsistentes"),
                   @ApiResponse(responseCode = "404", description = "Pedido, usuario editor, cliente, conductor o vehículo no encontrado")
               })
    Pedido actualizarPedido(
            @Parameter(description = "ID del pedido a actualizar", example = "1") Long id,
            @Parameter(description = "Objeto Pedido con los datos actualizados", required = true) Pedido pedidoActualizado,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Elimina físicamente un pedido y registra la auditoría",
               description = "Elimina un pedido de la base de datos.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Pedido eliminado exitosamente (no content)"),
                   @ApiResponse(responseCode = "404", description = "Pedido o usuario editor no encontrado")
               })
    void eliminarPedido(
            @Parameter(description = "ID del pedido a eliminar", example = "1") Long id,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Asigna un conductor y un vehículo a un pedido y registra la auditoría",
               description = "Asigna un conductor y un vehículo a un pedido existente. Puede cambiar el estado del pedido.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Conductor y vehículo asignados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de asignación inválidos (ej. conductor o vehículo no disponibles)"),
                   @ApiResponse(responseCode = "404", description = "Pedido, conductor, vehículo o usuario editor no encontrado")
               })
    Pedido asignarConductorYVehiculo(
            @Parameter(description = "ID del pedido a asignar", example = "1") Long pedidoId,
            @Parameter(description = "ID del conductor a asignar", example = "1") Long conductorId,
            @Parameter(description = "ID del vehículo a asignar", example = "1") Long vehiculoId,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Cambia el estado de un pedido y registra la auditoría",
               description = "Actualiza el estado de un pedido a un nuevo valor (ej. PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Estado del pedido actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
                   @ApiResponse(responseCode = "400", description = "Estado inválido o transición no permitida"),
                   @ApiResponse(responseCode = "404", description = "Pedido o usuario editor no encontrado")
               })
    Pedido cambiarEstadoPedido(
            @Parameter(description = "ID del pedido cuyo estado se va a cambiar", example = "1") Long pedidoId,
            @Parameter(description = "El nuevo estado del pedido (ej. \"EN_PROGRESO\")", required = true, example = "EN_PROGRESO") EstadoPedido  nuevoEstado,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Busca pedidos por el ID del cliente",
               description = "Retorna una lista de pedidos realizados por un cliente específico.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorCliente(
            @Parameter(description = "ID del cliente", example = "1") Long clienteId
    );

    @Operation(summary = "Busca pedidos por el ID del conductor asignado",
               description = "Retorna una lista de pedidos asignados a un conductor específico.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorConductor(
            @Parameter(description = "ID del conductor", example = "1") Long conductorId
    );

    @Operation(summary = "Busca pedidos por su estado",
               description = "Retorna una lista de pedidos que se encuentran en un estado particular (ej. PENDIENTE, COMPLETADO).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorEstado(
            @Parameter(description = "Estado del pedido (ej. PENDIENTE, EN_PROGRESO)", example = "PENDIENTE") EstadoPedido estado
    );

    @Operation(summary = "Busca pedidos creados dentro de un rango de fechas",
               description = "Retorna una lista de pedidos cuya fecha de creación se encuentra dentro del rango especificado.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorRangoFechasCreacion(
            @Parameter(description = "Fecha y hora de inicio del rango (formato ISO 8601)", example = "2023-01-01T00:00:00") LocalDateTime fechaInicio,
            @Parameter(description = "Fecha y hora de fin del rango (formato ISO 8601)", example = "2023-12-31T23:59:59") LocalDateTime fechaFin
    );

    @Operation(summary = "Obtiene el historial de cambios para un pedido específico",
               description = "Retorna una lista de registros de auditoría para un pedido específico, ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoAudit.class)))
               })
    List<PedidoAudit> obtenerHistorialCambiosPorPedido(
            @Parameter(description = "ID del pedido para obtener su historial", example = "1") Long pedidoId
    );

    @Operation(summary = "Obtiene el historial de cambios de pedidos por el nombre de usuario del editor",
               description = "Retorna una lista de registros de auditoría de pedidos realizados por un usuario editor específico.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoAudit.class)))
               })
    List<PedidoAudit> obtenerHistorialCambiosPorUsuarioEditor(
            @Parameter(description = "Nombre de usuario del editor", example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Obtiene pedidos de un conductor por estado",
               description = "Retorna una lista de pedidos asignados a un conductor específico con un estado determinado.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorConductorYEstado(
            @Parameter(description = "ID del conductor", example = "1") Long conductorId,
            @Parameter(description = "Estado del pedido (ej. EN_PROGRESO)", example = "EN_PROGRESO") EstadoPedido estado
    );

    @Operation(summary = "Obtiene pedidos de un cliente por estado",
               description = "Retorna una lista de pedidos realizados por un cliente específico con un estado determinado.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Pedidos encontrados exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
               })
    List<Pedido> obtenerPedidosPorClienteYEstado(
            @Parameter(description = "ID del cliente", example = "1") Long clienteId,
            @Parameter(description = "Estado del pedido (ej. PENDIENTE)", example = "PENDIENTE") EstadoPedido estado
    );

    @Operation(summary = "Verifica si un pedido pertenece a un cliente específico",
                description = "Comprueba si un pedido con el ID dado está asociado al cliente con el ID proporcionado.",
                responses = {
                    @ApiResponse(responseCode = "200", description = "Resultado de la verificación",
                                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class)))
                })
    boolean esPedidoPertenecienteACliente(
            @Parameter(description = "ID del pedido a verificar", example = "1") Long pedidoId,
            @Parameter(description = "ID del cliente propietario", example = "101") Long clienteId
    );

    @Operation(summary = "Verifica si un pedido está asignado a un conductor específico",
                description = "Comprueba si un pedido con el ID dado está asignado al conductor con el ID proporcionado.",
                responses = {
                    @ApiResponse(responseCode = "200", description = "Resultado de la verificación",
                                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class)))
                })
    boolean esPedidoAsignadoAConductor(
            @Parameter(description = "ID del pedido a verificar", example = "1") Long pedidoId,
            @Parameter(description = "ID del conductor asignado", example = "201") Long conductorId
    );

    @Operation(summary = "Obtiene todos los estados de pedido disponibles",
                description = "Retorna una lista de todos los posibles estados en los que puede encontrarse un pedido.",
                responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de estados obtenida exitosamente",
                                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = EstadoPedido.class)))
                })
    List<EstadoPedido> obtenerTodosLosEstadosDePedido();
}