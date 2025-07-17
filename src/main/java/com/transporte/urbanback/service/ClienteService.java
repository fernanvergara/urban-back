package com.transporte.urbanback.service;

import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit; 

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "ClienteService", description = "Servicio para la gestión de clientes y su auditoría")
public interface ClienteService {

    @Operation(summary = "Crea un nuevo cliente y registra la auditoría",
               description = "Guarda un nuevo cliente en la base de datos y crea un registro de auditoría de tipo CREAR. El usuario asociado debe existir previamente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Cliente creado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos, identificación duplicada o usuario ya asociado a otro cliente"),
                   @ApiResponse(responseCode = "404", description = "Usuario editor o usuario asociado al cliente no encontrado")
               })
    Cliente crearCliente(
            @Parameter(description = "Datos del cliente a crear, incluyendo el ID de su usuario asociado (si aplica)", required = true) Cliente cliente,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Obtiene un cliente por su ID",
               description = "Busca y retorna un cliente específico por su identificador único.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Cliente encontrado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
               })
    Optional<Cliente> obtenerClientePorId(
            @Parameter(description = "ID del cliente a buscar", example = "1") Long id
    );

    @Operation(summary = "Obtiene una lista de todos los clientes",
               description = "Retorna una lista de todos los clientes registrados en el sistema, activos o inactivos.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    List<Cliente> obtenerTodosLosClientes();

    @Operation(summary = "Obtiene una lista paginada de todos los clientes",
               description = "Retorna una lista paginada de todos los clientes registrados en el sistema, activos o inactivos.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de clientes obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    Page<Cliente> obtenerTodosLosClientesPaginados(Pageable pageable);

    @Operation(summary = "Actualiza un cliente existente y registra la auditoría",
               description = "Actualiza los datos de un cliente. La identificación o el usuario asociado no pueden ser duplicados en otros clientes.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos, identificación o usuario duplicado"),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado")
               })
    Cliente actualizarCliente(
            @Parameter(description = "ID del cliente a actualizar", example = "1") Long id,
            @Parameter(description = "Nuevos datos del cliente", required = true) Cliente clienteActualizado,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Elimina físicamente un cliente y registra la auditoría",
               description = "Elimina un cliente de la base de datos. Puede fallar si existen relaciones con otras entidades (ej. pedidos). Se recomienda usar la inactivación lógica.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente (no content)"),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado"),
                   @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el cliente debido a registros relacionados")
               })
    void eliminarCliente(
            @Parameter(description = "ID del cliente a eliminar", example = "1") Long id,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Cambia el estado activo/inactivo de un cliente y registra la auditoría",
               description = "Actualiza el estado 'activo' de un cliente (eliminación/activación lógica).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Estado del cliente actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class))),
                   @ApiResponse(responseCode = "404", description = "Cliente o usuario editor no encontrado")
               })
    Cliente cambiarEstadoActivoCliente(
            @Parameter(description = "ID del cliente a cambiar de estado", example = "1") Long id,
            @Parameter(description = "Nuevo estado de actividad (true para activo, false para inactivo)", required = true, example = "true") Boolean nuevoEstado,
            @Parameter(description = "Username del usuario que realiza la operación (ej. admin_user)", required = true) String usernameEditor
    );

    @Operation(summary = "Obtiene todos los clientes activos",
               description = "Retorna una lista de clientes que están marcados como activos.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes activos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    List<Cliente> obtenerClientesActivos();

    @Operation(summary = "Obtiene todos los clientes inactivos",
               description = "Retorna una lista de clientes que están marcados como inactivos (lógicamente eliminados).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de clientes inactivos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cliente.class)))
               })
    List<Cliente> obtenerClientesInactivos();

    @Operation(summary = "Obtiene el historial de cambios de un cliente por su ID",
               description = "Retorna una lista de registros de auditoría para un cliente específico, ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteAudit.class)))
               })
    List<ClienteAudit> obtenerHistorialCambiosPorCliente(
            @Parameter(description = "ID del cliente para obtener su historial", example = "1") Long clienteId
    );

    @Operation(summary = "Obtiene el historial de cambios de un cliente por su identificación",
               description = "Retorna una lista de registros de auditoría para un cliente específico, buscando por su identificación y ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteAudit.class)))
               })
    List<ClienteAudit> obtenerHistorialCambiosPorIdentificacion(
            @Parameter(description = "Identificación del cliente para obtener su historial", example = "123456789") String identificacion
    );

}