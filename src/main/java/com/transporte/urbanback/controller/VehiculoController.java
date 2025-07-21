package com.transporte.urbanback.controller;

import com.transporte.urbanback.dto.VehiculoAuditDTO;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException; 
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

@Tag(name = "VehiculoController", description = "API para la gestión de Vehículos")
@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final SecurityUtils securityUtils;

    @Autowired
    public VehiculoController(VehiculoService vehiculoService, SecurityUtils securityUtils) {
        this.vehiculoService = vehiculoService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo vehículo")
    @ApiResponse(responseCode = "201", description = "Vehículo creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de vehículo inválidos o placa ya existente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehiculo> crearVehiculo(@Valid @RequestBody Vehiculo vehiculo) {
        // Las excepciones IllegalArgumentException y EntityNotFoundException
        // serán capturadas por el GlobalExceptionHandler
        Vehiculo nuevoVehiculo = vehiculoService.crearVehiculo(vehiculo, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(nuevoVehiculo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener vehículo por ID", description = "Retorna un vehículo específico por su ID.")
    @ApiResponse(responseCode = "200", description = "Vehículo encontrado")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isVehiculoIdAssignedToCurrentUser(#id))")
    public ResponseEntity<Vehiculo> obtenerVehiculoPorId(@PathVariable Long id) {
        // EntityNotFoundException será capturada por el GlobalExceptionHandler
        return vehiculoService.obtenerVehiculoPorId(id)
                .map(vehiculo -> new ResponseEntity<>(vehiculo, HttpStatus.OK))
                .orElseThrow(() -> new EntityNotFoundException("Vehículo no encontrado con ID: " + id));
    }

    @GetMapping("/todos")
    @Operation(summary = "Obtener todos los vehículos", description = "Retorna una lista de todos los vehículos registrados.")
    @ApiResponse(responseCode = "200", description = "Lista de vehículos obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Vehiculo>> obtenerTodosLosVehiculos() {
        List<Vehiculo> vehiculos = vehiculoService.obtenerTodosLosVehiculos();
        return new ResponseEntity<>(vehiculos, HttpStatus.OK);
    }

    @GetMapping("/todos/paginado")
    @Operation(summary = "Obtener todos los vehículos con paginación",
               description = "Retorna una lista paginada de todos los vehículos registrados. " +
                             "Se pueden usar parámetros como 'page', 'size' y 'sort'.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Lista paginada de vehículos obtenida exitosamente")
               })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Vehiculo>> obtenerTodosLosVehiculosPaginados(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Vehiculo> vehiculos = vehiculoService.obtenerTodosLosVehiculosPaginados(pageable);
        return new ResponseEntity<>(vehiculos, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un vehículo existente", description = "Actualiza los datos de un vehículo existente por su ID.")
    @ApiResponse(responseCode = "200", description = "Vehículo actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de vehículo inválidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehiculo> actualizarVehiculo(
            @PathVariable Long id,
            @Valid @RequestBody Vehiculo vehiculoDetalles) {
        // Las excepciones EntityNotFoundException, IllegalArgumentException y SecurityException
        // serán capturadas por el GlobalExceptionHandler
        Vehiculo vehiculoActualizado = vehiculoService.actualizarVehiculo(id, vehiculoDetalles, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(vehiculoActualizado, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un vehículo", description = "Elimina un vehículo de la base de datos por su ID.")
    @ApiResponse(responseCode = "204", description = "Vehículo eliminado exitosamente")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto: No se puede eliminar el vehículo debido a registros relacionados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Long id) {
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        vehiculoService.eliminarVehiculo(id, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/estado/{id}")
    @Operation(summary = "Cambiar el estado activo de un vehículo",
                description = "Permite inactivar (eliminar lógicamente) o reactivar un vehículo por su ID.")
    @ApiResponse(responseCode = "200", description = "Estado del vehículo actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    @ApiResponse(responseCode = "400", description = "Estado inválido proporcionado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehiculo> cambiarEstadoActivoVehiculo(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        // Las excepciones EntityNotFoundException y IllegalStateException
        // serán capturadas por el GlobalExceptionHandler
        Vehiculo vehiculoActualizado = vehiculoService.cambiarEstadoActivoVehiculo(id, activo, securityUtils.obtenerNombreUsuarioAutenticado());
        return new ResponseEntity<>(vehiculoActualizado, HttpStatus.OK);
    }

    @GetMapping("/activos")
    @Operation(summary = "Obtener vehículos activos", description = "Retorna una lista de todos los vehículos activos.")
    @ApiResponse(responseCode = "200", description = "Lista de vehículos activos obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENTE') or hasRole('CONDUCTOR')")
    public ResponseEntity<List<Vehiculo>> obtenerVehiculosActivos() {
        List<Vehiculo> vehiculos = vehiculoService.obtenerVehiculosActivos();
        return new ResponseEntity<>(vehiculos, HttpStatus.OK);
    }

    @GetMapping("/inactivos")
    @Operation(summary = "Obtener vehículos inactivos", description = "Retorna una lista de todos los vehículos inactivos.")
    @ApiResponse(responseCode = "200", description = "Lista de vehículos inactivos obtenida exitosamente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Vehiculo>> obtenerVehiculosInactivos() {
        List<Vehiculo> vehiculos = vehiculoService.obtenerVehiculosInactivos();
        return new ResponseEntity<>(vehiculos, HttpStatus.OK);
    }

    @GetMapping("/auditoria/{vehiculoId}")
    @Operation(summary = "Obtener historial de cambios de un vehículo por ID", description = "Retorna el historial de auditoría de un vehículo específico.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isVehiculoIdAssignedToCurrentUser(#vehiculoId))")
    public ResponseEntity<List<VehiculoAuditDTO>> obtenerHistorialCambiosPorVehiculo(@PathVariable Long vehiculoId) {
        List<VehiculoAudit> historial = vehiculoService.obtenerHistorialCambiosPorVehiculo(vehiculoId);
        // Si el historial está vacío y el vehículo no existe, se lanza 404
        if (historial.isEmpty() && !vehiculoService.obtenerVehiculoPorId(vehiculoId).isPresent()) {
            throw new EntityNotFoundException("Vehículo no encontrado con ID: " + vehiculoId);
        }
        return new ResponseEntity<>(mapeoLista(historial), HttpStatus.OK);
    }

    @GetMapping("/auditoria/placa/{placa}")
    @Operation(summary = "Obtener historial de cambios de un vehículo por placa", description = "Retorna el historial de auditoría de un vehículo por su placa.")
    @ApiResponse(responseCode = "200", description = "Historial de cambios obtenido exitosamente")
    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado por placa")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONDUCTOR') and @securityUtils.isVehiculoPlacaAssignedToCurrentUser(#placa))")
    public ResponseEntity<List<VehiculoAuditDTO>> obtenerHistorialCambiosPorPlaca(@PathVariable String placa) {
        List<VehiculoAudit> historial = vehiculoService.obtenerHistorialCambiosPorPlaca(placa);
        // Si el historial está vacío y el vehículo no existe, se lanza 404
        if (historial.isEmpty() && !vehiculoService.obtenerVehiculoPorPlaca(placa).isPresent()) {
             throw new EntityNotFoundException("Vehículo no encontrado con placa: " + placa);
        }
        return new ResponseEntity<>(mapeoLista(historial), HttpStatus.OK);
    }

    private List<VehiculoAuditDTO> mapeoLista(List<VehiculoAudit> lista) {
        return lista.stream()
                .map(audit -> {
                    VehiculoAuditDTO dto = new VehiculoAuditDTO();
                    dto.setId(audit.getId());
                    dto.setVehiculoId(audit.getVehiculo() != null ? audit.getVehiculo().getId() : null);
                    dto.setDetallesCambio(audit.getDetallesCambio());
                    dto.setTipoOperacion(audit.getTipoOperacion().name());
                    dto.setFechaCambio(audit.getFechaCambio());
                    dto.setUsuarioEditor(audit.getUsuarioEditor() == null ? null : audit.getUsuarioEditor().getUsername());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
