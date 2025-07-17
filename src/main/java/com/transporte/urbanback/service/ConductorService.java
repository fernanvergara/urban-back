package com.transporte.urbanback.service;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "ConductorService", description = "Servicio para la gestión de conductores y su auditoría")
public interface ConductorService {

    @Operation(summary = "Crea un nuevo conductor y registra la auditoría",
               description = "Guarda un nuevo conductor en la base de datos y crea un registro de auditoría de tipo CREAR.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Conductor creado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de conductor inválidos o identificación/usuario ya existente"),
                   @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado")
               })
    Conductor crearConductor(
            @Parameter(description = "Datos del conductor a crear, incluyendo su usuario asociado", required = true) Conductor conductor,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Obtiene un conductor por su ID",
               description = "Busca y retorna un conductor específico por su identificador único.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Conductor encontrado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
               })
    Optional<Conductor> obtenerConductorPorId(
            @Parameter(description = "ID del conductor a buscar", example = "1") Long id
    );

    @Operation(summary = "Obtiene todos los conductores",
               description = "Retorna una lista de todos los conductores registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de conductores obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
               })
    List<Conductor> obtenerTodosLosConductores();

    @Operation(summary = "Obtiene una lista paginada todos los conductores",
               description = "Retorna una lista paginada todos los conductores registrados en el sistema.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de conductores obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
               })
    Page<Conductor> obtenerTodosLosConductoresPaginados(Pageable pageable);

    @Operation(summary = "Actualiza un conductor existente y registra la auditoría",
               description = "Actualiza los datos de un conductor. La identificación no puede ser duplicada.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Conductor actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "400", description = "Datos de conductor inválidos o identificación duplicada"),
                   @ApiResponse(responseCode = "404", description = "Conductor o usuario editor no encontrado")
               })
    Conductor actualizarConductor(
            @Parameter(description = "ID del conductor a actualizar", example = "1") Long id,
            @Parameter(description = "Nuevos datos del conductor", required = true) Conductor conductorActualizado,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Elimina físicamente un conductor y registra la auditoría",
               description = "Elimina un conductor de la base de datos. Puede fallar si existen relaciones con otras entidades (ej. vehículos asignados). Se recomienda usar la inactivación lógica.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Conductor eliminado exitosamente (no content)"),
                   @ApiResponse(responseCode = "404", description = "Conductor o usuario editor no encontrado"),
                   @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el conductor debido a registros relacionados o vehículos asignados")
               })
    void eliminarConductor(
            @Parameter(description = "ID del conductor a eliminar", example = "1") Long id,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Cambia el estado activo/inactivo de un conductor y registra la auditoría",
               description = "Actualiza el estado 'activo' de un conductor (eliminación/activación lógica).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Estado del conductor actualizado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "404", description = "Conductor o usuario editor no encontrado")
               })
    Conductor cambiarEstadoActivoConductor(
            @Parameter(description = "ID del conductor a cambiar de estado", example = "1") Long id,
            @Parameter(description = "Nuevo estado de actividad (true para activo, false para inactivo)", required = true, example = "true") Boolean nuevoEstado,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    // Los métodos de asignar/desasignar vehículos modifican el vehículo, no el conductor directamente.
    // Aunque el conductor service es quien contiene la lógica de negocio del limite de 3 vehiculos
    @Operation(summary = "Asigna un vehículo a un conductor y registra la auditoría",
               description = "Asigna un vehículo a un conductor, validando la capacidad del conductor (máximo 3 vehículos).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo asignado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "400", description = "Límite de vehículos alcanzado, vehículo ya asignado o vehículo inactivo"),
                   @ApiResponse(responseCode = "404", description = "Conductor, vehículo o usuario editor no encontrado")
               })
    Conductor asignarVehiculo(
            @Parameter(description = "ID del conductor al que se asignará el vehículo", example = "1") Long conductorId,
            @Parameter(description = "ID del vehículo a asignar", example = "101") Long vehiculoId,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Desasigna un vehículo de un conductor y registra la auditoría",
               description = "Desasigna un vehículo de un conductor.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Vehículo desasignado exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class))),
                   @ApiResponse(responseCode = "400", description = "Vehículo no asignado al conductor especificado"),
                   @ApiResponse(responseCode = "404", description = "Conductor, vehículo o usuario editor no encontrado")
               })
    Conductor desasignarVehiculo(
            @Parameter(description = "ID del conductor del que se desasignará el vehículo", example = "1") Long conductorId,
            @Parameter(description = "ID del vehículo a desasignar", example = "101") Long vehiculoId,
            @Parameter(description = "Username del usuario que realiza la operación", required = true, example = "admin_user") String usernameEditor
    );

    @Operation(summary = "Obtiene todos los conductores activos",
               description = "Retorna una lista de conductores que están marcados como activos.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de conductores activos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
               })
    List<Conductor> obtenerConductorActivos();

    @Operation(summary = "Obtiene todos los conductores inactivos",
               description = "Retorna una lista de conductores que están marcados como inactivos (lógicamente eliminados).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista de conductores inactivos obtenida exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
               })
    List<Conductor> obtenerConductorInactivos();

    @Operation(summary = "Obtiene el historial de cambios de un conductor por su ID",
               description = "Retorna una lista de registros de auditoría para un conductor específico, ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConductorAudit.class)))
               })
    List<ConductorAudit> obtenerHistorialCambiosPorConductor(
            @Parameter(description = "ID del conductor para obtener su historial", example = "1") Long conductorId
    );

    @Operation(summary = "Obtiene el historial de cambios de un conductor por su identificación",
               description = "Retorna una lista de registros de auditoría para un conductor específico, buscando por su identificación y ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConductorAudit.class)))
               })
    List<ConductorAudit> obtenerHistorialCambiosPorIdentificacion(
            @Parameter(description = "Identificación del conductor para obtener su historial", example = "123456789") String identificacion
    );

    @Operation(summary = "Obtiene el historial de cambios de un conductor por parte de su nombre",
               description = "Retorna una lista de registros de auditoría para conductores cuyo nombre contenga la cadena proporcionada, ordenados por fecha de cambio descendente.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConductorAudit.class)))
               })
    List<ConductorAudit> obtenerHistorialCambiosPorNombre(
            @Parameter(description = "Parte del nombre completo del conductor para obtener su historial", example = "Juan") String nombre
    );
}