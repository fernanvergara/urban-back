package com.transporte.urbanback.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Collections; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.constants.Constantes;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.exception.ResourceNotFoundException; 
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.ConductorAuditRepository;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.VehiculoRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.ConductorService;
import com.transporte.urbanback.utilidades.Utilidades;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConductorServiceImpl implements ConductorService {

    private final ConductorRepository conductorRepository;
    private final ConductorAuditRepository conductorAuditRepository;
    private final VehiculoRepository vehiculoRepository; 
    private final ObjectMapper objectMapper;

    @Autowired
    public ConductorServiceImpl(ConductorRepository conductorRepository,
                                ConductorAuditRepository conductorAuditRepository,
                                VehiculoRepository vehiculoRepository,
                                ObjectMapper objectMapper) {
        this.conductorRepository = conductorRepository;
        this.conductorAuditRepository = conductorAuditRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.objectMapper = objectMapper;
    }

    private void registrarAuditoria(Conductor conductor, TipoOperacion tipoOperacion, Usuario usuarioEditor) {
        if (conductor == null) {
            log.error("Advertencia: Intento de registrar auditoría para un conductor nulo. Operación ignorada.");
            return;
        }

        String detallesCambio = "";
        if (tipoOperacion != TipoOperacion.ELIMINAR) {
            try {
                // Serializar el objeto Conductor a JSON.
                detallesCambio = objectMapper.writeValueAsString(conductor);
            } catch (Exception e) {
                log.error("Error al serializar Conductor para auditoría: " + e.getMessage());
                detallesCambio = "{ \"error\": \"No se pudo serializar el objeto\" }";
            }
        } else {
            detallesCambio = "{}"; // JSON vacío para eliminación lógica
        }

        ConductorAudit audit = new ConductorAudit(conductor, tipoOperacion, usuarioEditor, detallesCambio);
        conductorAuditRepository.save(audit);
    }

    @Override
    @Transactional
    public Conductor crearConductor(Conductor conductor, String usernameEditor) {
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);
        if (conductorRepository.findByIdentificacion(conductor.getIdentificacion()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un conductor con la identificación: " + conductor.getIdentificacion());
        }

        Conductor nuevoConductor = conductorRepository.save(conductor);
        registrarAuditoria(nuevoConductor, TipoOperacion.CREAR, editor);
        return nuevoConductor;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conductor> obtenerConductorPorId(Long id) {
        return conductorRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conductor> obtenerTodosLosConductores() {
        return conductorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conductor> obtenerTodosLosConductoresPaginados(Pageable pageable) {
        return conductorRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Conductor actualizarConductor(Long id, Conductor conductorActualizado, String usernameEditor) {
        return conductorRepository.findById(id).map(conductorExistente -> {
            Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

            if (!conductorExistente.getIdentificacion().equalsIgnoreCase(conductorActualizado.getIdentificacion())) {
                if (!editor.getRol().equals(Rol.ADMIN)) {
                    throw new SecurityException("Solo un administrador puede cambiar la identificación de un conductor.");
                }
                if (conductorRepository.findByIdentificacion(conductorActualizado.getIdentificacion()).isPresent()) {
                    throw new IllegalArgumentException("La nueva identificación ya está registrada en otro conductor.");
                }
                conductorExistente.setIdentificacion(conductorActualizado.getIdentificacion());
            }

            conductorExistente.setNombreCompleto(conductorActualizado.getNombreCompleto());
            conductorExistente.setFechaNacimiento(conductorActualizado.getFechaNacimiento());
            conductorExistente.setTelefono(conductorActualizado.getTelefono());

            Conductor conductorGuardado = conductorRepository.save(conductorExistente);
            registrarAuditoria(conductorGuardado, TipoOperacion.ACTUALIZAR, editor);
            return conductorGuardado;
        }).orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminarConductor(Long id, String usernameEditor) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + id));
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        try {
            // Antes de eliminar físicamente, asegúrate de que no tenga vehículos asignados.
            // Contamos los vehículos que tienen a este conductor asignado.
            long vehiculosAsignadosCount = vehiculoRepository.countByConductor(conductor);
            if (vehiculosAsignadosCount > 0) {
                throw new IllegalStateException("No se puede eliminar el conductor con ID " + id + " porque tiene " + vehiculosAsignadosCount + " vehículos asignados. " +
                                                "Desasígnelos primero o considere inactivarlo lógicamente.");
            }
            conductorRepository.delete(conductor);
            registrarAuditoria(conductor, TipoOperacion.ELIMINAR, editor);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el conductor con ID " + id + " debido a una violación de integridad referencial. " +
                                            "Asegúrese de que no esté relacionado con otros registros.", e);
        } catch (Exception e) { 
            throw new IllegalStateException("Error al intentar eliminar el conductor con ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Conductor cambiarEstadoActivoConductor(Long id, Boolean nuevoEstado, String usernameEditor) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + id)); 
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        if (conductor.getActivo() != nuevoEstado) {
            int updatedRows = conductorRepository.updateActivoStatus(id, nuevoEstado);
            if (updatedRows > 0) {
                conductor.setActivo(nuevoEstado); 
                registrarAuditoria(conductor, TipoOperacion.CAMBIO_ESTADO, editor); 
                return conductor;
            } else {
                throw new IllegalStateException("No se pudo actualizar el estado del conductor con ID: " + id); 
            }
        }
        return conductor; 
    }

    @Override
    @Transactional
    public Conductor asignarVehiculo(Long conductorId, Long vehiculoId, String usernameEditor) {
        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + conductorId)); 
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo no encontrado con ID: " + vehiculoId)); 
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        if (!vehiculo.getActivo()) {
            throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " está inactivo y no puede ser asignado.");
        }

        // Contar vehículos ya asignados a este conductor
        long vehiculosAsignadosCount = vehiculoRepository.countByConductor(conductor);

        // Regla de negocio: un conductor no puede estar asociado a más de 3 vehículos
        if (vehiculosAsignadosCount >= Constantes.MAX_VEHICULOS_POR_CONDUCTOR) {
            throw new IllegalStateException("El conductor ya tiene el máximo de " + Constantes.MAX_VEHICULOS_POR_CONDUCTOR + " vehículos asignados.");
        }

        if (vehiculo.getConductor() != null) {
            if (vehiculo.getConductor().getId().equals(conductorId)) {
                throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " ya está asignado a este conductor.");
            } else {
                throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " ya está asignado a otro conductor (ID: " + vehiculo.getConductor().getId() + ").");
            }
        }

        vehiculo.setConductor(conductor);
        vehiculoRepository.save(vehiculo); 

        Conductor conductorActualizado = conductorRepository.findById(conductorId)
            .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + conductorId)); 
        registrarAuditoria(conductorActualizado, TipoOperacion.ACTUALIZAR, editor); 

        return conductorActualizado; 
    }

    @Override
    @Transactional
    public Conductor desasignarVehiculo(Long conductorId, Long vehiculoId, String usernameEditor) {
        Conductor conductorActualizado = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + conductorId)); 
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo no encontrado con ID: " + vehiculoId)); 
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        if (vehiculo.getConductor() == null || !vehiculo.getConductor().getId().equals(conductorId)) {
            throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " no está asignado al conductor con ID " + conductorId + ".");
        }

        vehiculo.setConductor(null);
        vehiculoRepository.save(vehiculo); 

        registrarAuditoria(conductorActualizado, TipoOperacion.ACTUALIZAR, editor); 
        
        return conductorActualizado; 
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conductor> obtenerConductorActivos() {
        return conductorRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conductor> obtenerConductorInactivos() {
        return conductorRepository.findByActivoFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorAudit> obtenerHistorialCambiosPorConductor(Long conductorId) {
        return conductorAuditRepository.findByConductorIdOrderByFechaCambioDesc(conductorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorAudit> obtenerHistorialCambiosPorIdentificacion(String identificacion) {
        Optional<Conductor> conductorOpt = conductorRepository.findByIdentificacion(identificacion);
        if (conductorOpt.isEmpty()) {
           return Collections.emptyList();
        }
        return conductorAuditRepository.findByConductorIdentificacionOrderByFechaCambioDesc(conductorOpt.get().getIdentificacion());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorAudit> obtenerHistorialCambiosPorNombre(String nombre) {
        return conductorAuditRepository.findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc(nombre);
    }
}
