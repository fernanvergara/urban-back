package com.transporte.urbanback.controller;

import com.transporte.urbanback.dto.ConductorAuditDTO;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.ConductorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException; 

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "ConductorController", description = "API para la gestión de Conductores")
@RestController
@RequestMapping("/api/v1/conductores")
public class ConductorController {

    private final ConductorService conductorService;
    private final SecurityUtils securityUtils;

    @Autowired
    public ConductorController(ConductorService conductorService, SecurityUtils securityUtils) {
        this.conductorService = conductorService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/todos")
    @Operation(summary = "Obtener todos los conductores", description = "Retorna una lista de todos los conductores registrados.")
    @ApiResponse(responseCode = "200", description = "Lista de conductores obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Conductor>> obtenerTodosLosConductores() {
        List<Conductor> conductores = conductorService.obtenerTodosLosConductores();
        return new ResponseEntity<>(conductores, HttpStatus.OK);
    }

    @GetMapping("/todos/paginado")
    @Operation(summary = "Obtener todos los conductores con paginación",
               description = "Retorna una lista paginada de todos los conductores registrados. " +
                             "Se pueden usar parámetros como 'page', 'size' y 'sort'.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de conductores obtenida exitosamente")
               })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Conductor>> obtenerTodosLosConductoresPaginados(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Conductor> conductores = conductorService.obtenerTodosLosConductoresPaginados(pageable);
        return new ResponseEntity<>(conductores, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener conductor por ID", description = "Retorna un conductor específico por su ID.")
    @ApiResponse(responseCode = "200", description = "Conductor encontrado")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdLinkedToCurrentUser(#id))")
    public ResponseEntity<Conductor> obtenerConductorPorId(@PathVariable Long id) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return conductorService.obtenerConductorPorId(id)
                .map(conductor -> new ResponseEntity<>(conductor, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Conductor no encontrado con ID: " + id));
    }

    @GetMapping("/ident/{identificacion}")
    @Operation(summary = "Obtener conductor por Identificación", description = "Retorna un conductor específico por su Identificación.")
    @ApiResponse(responseCode = "200", description = "Conductor encontrado")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdLinkedToCurrentUser(#id))")
    public ResponseEntity<Conductor> obtenerConductorPorIdentificacion(@PathVariable String identificacion) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return conductorService.obtenerConductorPorIdentificacion(identificacion)
                .map(conductor -> new ResponseEntity<>(conductor, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Conductor no encontrado con Identificación: " + identificacion));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo conductor", description = "Crea un nuevo conductor en la base de datos.")
    @ApiResponse(responseCode = "201", description = "Conductor creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de conductor inválidos")
    @ApiResponse(responseCode = "404", description = "Usuario editor no encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Conductor> crearConductor(
            @Valid @RequestBody Conductor conductor) {
            // Las excepciones IllegalArgumentException y EntityNotFoundException
            // serán capturadas por el GlobalExceptionHandler
            Conductor nuevoConductor = conductorService.crearConductor(conductor, securityUtils.obtenerNombreUsuarioAutenticado());
            return new ResponseEntity<>(nuevoConductor, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un conductor existente", description = "Actualiza los datos de un conductor existente por su ID.")
    @ApiResponse(responseCode = "200", description = "Conductor actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de conductor inválidos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (ej. no ADMIN intentando cambiar identificación)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Conductor> actualizarConductor(
            @PathVariable Long id,
            @Valid @RequestBody Conductor conductorDetalles) {
            // Las excepciones EntityNotFoundException, IllegalArgumentException y SecurityException
            // serán capturadas por el GlobalExceptionHandler
            Conductor conductorActualizado = conductorService.actualizarConductor(id, conductorDetalles, securityUtils.obtenerNombreUsuarioAutenticado());
            return new ResponseEntity<>(conductorActualizado, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un conductor", description = "Elimina un conductor de la base de datos por su ID.")
    @ApiResponse(responseCode = "204", description = "Conductor eliminado exitosamente")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el conductor debido a vehículos asignados o integridad referencial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarConductor(@PathVariable Long id) {
        // Las excepciones EntityNotFoundException, IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        conductorService.eliminarConductor(id, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/estado") // Usar PATCH para actualizar un campo parcial
    @Operation(summary = "Cambiar el estado activo de un conductor (eliminación lógica/activación)",
            description = "Permite inactivar (eliminar lógicamente) o reactivar un conductor por su ID.")
    @ApiResponse(responseCode = "200", description = "Estado del conductor actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @ApiResponse(responseCode = "400", description = "Estado inválido proporcionado")
    @ApiResponse(responseCode = "409", description = "Conflicto: No se pudo actualizar el estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Conductor> cambiarEstadoActivoConductor(
            @PathVariable Long id,
            @RequestParam Boolean activo) { // Recibir el nuevo estado (true/false)
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Conductor conductorActualizado = conductorService.cambiarEstadoActivoConductor(id, activo, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(conductorActualizado, HttpStatus.OK);
    }

    @PutMapping("/asignar/{conductorId}/vehiculo/{vehiculoId}")
    @Operation(summary = "Asigna un vehículo a un conductor",
               description = "Asigna un vehículo específico a un conductor, respetando el límite de vehículos por conductor.")
    @ApiResponse(responseCode = "200", description = "Vehículo asignado exitosamente al conductor",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. vehículo inactivo, ya asignado, límites alcanzados)")
    @ApiResponse(responseCode = "404", description = "Conductor o vehículo no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto: El conductor ya tiene el máximo de vehículos asignados o el vehículo ya está asignado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Conductor> asignarVehiculo(
            @PathVariable Long conductorId,
            @PathVariable Long vehiculoId) {
        // Las excepciones EntityNotFoundException, IllegalArgumentException, IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Conductor conductorActualizado = conductorService.asignarVehiculo(conductorId, vehiculoId, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(conductorActualizado, HttpStatus.OK);
    }

    @PutMapping("/desasignar/{conductorId}/vehiculo/{vehiculoId}")
    @Operation(summary = "Desasigna un vehículo de un conductor",
               description = "Desvincula un vehículo de un conductor específico.")
    @ApiResponse(responseCode = "200", description = "Vehículo desasignado exitosamente del conductor",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = Conductor.class)))
    @ApiResponse(responseCode = "400", description = "El vehículo no está asignado a este conductor")
    @ApiResponse(responseCode = "404", description = "Conductor o vehículo no encontrado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Conductor> desasignarVehiculo(
            @PathVariable Long conductorId,
            @PathVariable Long vehiculoId) {
        // Las excepciones EntityNotFoundException, IllegalArgumentException
        // serán capturadas por el GlobalExceptionHandler
        Conductor conductorActualizado = conductorService.desasignarVehiculo(conductorId, vehiculoId, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(conductorActualizado, HttpStatus.OK);
    }

    @GetMapping("/activos")
    @Operation(summary = "Obtener conductores activos", description = "Retorna una lista de conductores con estado 'activo' en true.")
    @ApiResponse(responseCode = "200", description = "Lista de conductores activos obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Conductor>> obtenerConductorActivos() {
        List<Conductor> conductores = conductorService.obtenerConductorActivos();
        return new ResponseEntity<>(conductores, HttpStatus.OK);
    }

    @GetMapping("/inactivos")
    @Operation(summary = "Obtener conductores inactivos", description = "Retorna una lista de conductores con estado 'activo' en false.")
    @ApiResponse(responseCode = "200", description = "Lista de conductores inactivos obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Conductor>> obtenerConductorInactivos() {
        List<Conductor> conductores = conductorService.obtenerConductorInactivos();
        return new ResponseEntity<>(conductores, HttpStatus.OK);
    }

    @GetMapping("/auditoria/{conductorId}")
    @Operation(summary = "Obtener historial de cambios de un conductor por ID", description = "Retorna el historial de auditoría de un conductor específico.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdLinkedToCurrentUser(#conductorId))")
    public ResponseEntity<List<ConductorAuditDTO>> obtenerHistorialPorIdConductor(@PathVariable Long conductorId) {
        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorConductor(conductorId);
        // Si el historial está vacío y el conductor no existe, se lanza 404
        if (historial.isEmpty() && !conductorService.obtenerConductorPorId(conductorId).isPresent()) {
            throw new EntityNotFoundException("Conductor no encontrado con ID: " + conductorId);
        }
        return new ResponseEntity<>(mapeoLista(historial), HttpStatus.OK);
    }

    @GetMapping("/auditoria/identificacion/{identificacion}")
    @Operation(summary = "Obtener historial de cambios de un conductor por identificación", description = "Retorna el historial de auditoría de un conductor por su identificación.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado por identificación")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isConductorIdentificacionLinkedToCurrentUser(#identificacion))")
    public ResponseEntity<List<ConductorAuditDTO>> obtenerHistorialPorIdentificacion(@PathVariable String identificacion) {
        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorIdentificacion(identificacion);
        // Si el historial está vacío se lanza 404
        if (historial.isEmpty() ) {
            throw new EntityNotFoundException("Conductor no encontrado con identificación: " + identificacion);
        }
        return new ResponseEntity<>(mapeoLista(historial), HttpStatus.OK);
    }

    @GetMapping("/auditoria/nombre/{nombre}")
    @Operation(summary = "Obtener historial de cambios de conductores por nombre", description = "Retorna el historial de auditoría de conductores cuyo nombre contenga el texto dado.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ConductorAuditDTO>> obtenerHistorialPorNombre(@PathVariable String nombre) {
        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorNombre(nombre);
        return new ResponseEntity<>(mapeoLista(historial), HttpStatus.OK);
    }

    private List<ConductorAuditDTO> mapeoLista(List<ConductorAudit> lista){
        return lista.stream()
                    .map(audit -> {
                        ConductorAuditDTO dto = new ConductorAuditDTO();
                        dto.setId(audit.getId());
                        dto.setConductorId(audit.getConductor().getId());
                        dto.setDetallesCambio(audit.getDetallesCambio());
                        dto.setTipoOperacion(audit.getTipoOperacion().name());
                        dto.setFechaCambio(audit.getFechaCambio());
                        dto.setUsuarioEditor(audit.getUsuarioEditor() == null ? null : audit.getUsuarioEditor().getUsername());
                        return dto;
                    })
                    .collect(Collectors.toList());
    }

}
