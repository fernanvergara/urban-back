package com.transporte.urbanback.service;

import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "VehiculoService", description = "Servicio para la gestión de vehículos y su auditoría")
public interface VehiculoService {

    @Operation(summary = "Crea un nuevo vehículo y registra la auditoría",
               description = "Guarda un nuevo vehículo en la base de datos y crea un registro de auditoría de tipo CREAR.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo creado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de vehículo inválidos o placa ya existente"),
                   @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado")
               })
    Vehiculo crearVehiculo(
            @Parameter(description = "Datos del vehículo a crear", required = true) Vehiculo vehiculo,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Obtiene un vehículo por su ID",
               description = "Busca y retorna un vehículo específico por su identificador único.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo encontrado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class))),
                   @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
               })
    Optional<Vehiculo> obtenerVehiculoPorId(
            @Parameter(description = "ID del vehículo a buscar", example = "1") Long id
    );

    @Operation(summary = "Obtiene un vehículo por su placa",
               description = "Busca y retorna un vehículo específico por su placa.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo encontrado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class))),
                   @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
               })
    Optional<Vehiculo> obtenerVehiculoPorPlaca(
            @Parameter(description = "Placa del vehículo a buscar", example = "ABC-123") String placa
    );

    @Operation(summary = "Obtiene todos los vehículos",
               description = "Retorna una lista de todos los vehículos registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de vehículos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class)))
               })
    List<Vehiculo> obtenerTodosLosVehiculos();

    @Operation(summary = "Obtiene una lista paginada de todos los vehículos",
               description = "Retorna una lista paginada de todos los vehículos registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de vehículos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class)))
               })
    Page<Vehiculo> obtenerTodosLosVehiculosPaginados(Pageable pageable);
    
    @Operation(summary = "Actualiza un vehículo existente y registra la auditoría",
               description = "Actualiza los datos de un vehículo. La placa solo puede ser modificada por un administrador.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de vehículo inválidos o nueva placa ya existente"),
                   @ApiResponse(responseCode = "403", description = "Permiso denegado para cambiar la placa (no es administrador)"),
                   @ApiResponse(responseCode = "404", description = "Vehículo o usuario editor no encontrado")
               })
    Vehiculo actualizarVehiculo(
            @Parameter(description = "ID del vehículo a actualizar", example = "1") Long id,
            @Parameter(description = "Nuevos datos del vehículo", required = true) Vehiculo vehiculoActualizado,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Elimina físicamente un vehículo y registra la auditoría",
               description = "Elimina un vehículo de la base de datos. Puede fallar si existen relaciones con otras entidades. Se recomienda usar la inactivación lógica.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Vehículo eliminado exitosamente (no content)"),
                   @ApiResponse(responseCode = "404", description = "Vehículo o usuario editor no encontrado"),
                   @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el vehículo debido a registros relacionados")
               })
    void eliminarVehiculo(
            @Parameter(description = "ID del vehículo a eliminar", example = "1") Long id,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Cambia el estado activo/inactivo de un vehículo y registra la auditoría",
               description = "Actualiza el estado 'activo' de un vehículo (eliminación/activación lógica).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Estado del vehículo actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class))),
                   @ApiResponse(responseCode = "404", description = "Vehículo o usuario editor no encontrado")
               })
    Vehiculo cambiarEstadoActivoVehiculo(
            @Parameter(description = "ID del vehículo a cambiar de estado", example = "1") Long id,
            @Parameter(description = "Nuevo estado de actividad (true para activo, false para inactivo)", required = true, example = "true") Boolean nuevoEstado,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Obtiene todos los vehículos activos",
               description = "Retorna una lista de vehículos que están marcados como activos.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de vehículos activos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class)))
               })
    List<Vehiculo> obtenerVehiculosActivos();

    @Operation(summary = "Obtiene todos los vehículos inactivos",
               description = "Retorna una lista de vehículos que están marcados como inactivos (lógicamente eliminados).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de vehículos inactivos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vehiculo.class)))
               })
    List<Vehiculo> obtenerVehiculosInactivos();

    @Operation(summary = "Obtiene el historial de cambios de un vehículo por su ID",
               description = "Retorna una lista de registros de auditoría para un vehículo específico, ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehiculoAudit.class)))
               })
    List<VehiculoAudit> obtenerHistorialCambiosPorVehiculo(
            @Parameter(description = "ID del vehículo para obtener su historial", example = "1") Long vehiculoId
    );

    @Operation(summary = "Obtiene el historial de cambios de un vehículo por su placa",
               description = "Retorna una lista de registros de auditoría para un vehículo específico, buscando por su placa y ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehiculoAudit.class)))
               })
    List<VehiculoAudit> obtenerHistorialCambiosPorPlaca(
            @Parameter(description = "Placa del vehículo para obtener su historial", example = "XYZ-789") String placa
    );
}