package com.transporte.urbanback.service.impl;

import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.repository.VehiculoAuditRepository;
import com.transporte.urbanback.repository.VehiculoRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.VehiculoService;
import com.transporte.urbanback.utilidades.Utilidades;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final VehiculoAuditRepository vehiculoAuditRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository,
                               VehiculoAuditRepository vehiculoAuditRepository,
                               ObjectMapper objectMapper) {
        this.vehiculoRepository = vehiculoRepository;
        this.vehiculoAuditRepository = vehiculoAuditRepository;
        this.objectMapper = objectMapper;
    }

    private void registrarAuditoria(Vehiculo vehiculo, TipoOperacion tipoOperacion, Usuario usuarioEditor) {
        if (vehiculo == null) {
            log.error("Advertencia: Intento de registrar auditoría para un vehículo nulo. Operación ignorada.");
            return;
        }
        String detallesCambio = "";
        if (tipoOperacion != TipoOperacion.ELIMINAR) {
            try {
                detallesCambio = objectMapper.writeValueAsString(vehiculo);
            } catch (Exception e) {
                log.error("Error al serializar Vehiculo para auditoría: " + e.getMessage());
                detallesCambio = "{ \"error\": \"No se pudo serializar el objeto\" }";
            }
        } else if (tipoOperacion == TipoOperacion.ELIMINAR) {
            detallesCambio = "{}";
        }

        VehiculoAudit audit = new VehiculoAudit(vehiculo, tipoOperacion, usuarioEditor, detallesCambio);
        vehiculoAuditRepository.save(audit);
    }

    @Override
    @Transactional
    public Vehiculo crearVehiculo(Vehiculo vehiculo, String usernameEditor) {
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);
        if (vehiculoRepository.findByPlaca(vehiculo.getPlaca()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un vehículo con la placa: " + vehiculo.getPlaca());
        }
        Vehiculo nuevoVehiculo = vehiculoRepository.save(vehiculo);
        registrarAuditoria(nuevoVehiculo, TipoOperacion.CREAR, editor);
        return nuevoVehiculo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehiculo> obtenerVehiculoPorId(Long id) {
        return vehiculoRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehiculo> obtenerVehiculoPorPlaca(String placa) {
        return vehiculoRepository.findByPlaca(placa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehiculo> obtenerTodosLosVehiculos() {
        return vehiculoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Vehiculo> obtenerTodosLosVehiculosPaginados(Pageable pageable){
        return vehiculoRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Vehiculo actualizarVehiculo(Long id, Vehiculo vehiculoActualizado, String usernameEditor) {
        return vehiculoRepository.findById(id).map(vehiculoExistente -> {
            Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

            if (!vehiculoExistente.getPlaca().equalsIgnoreCase(vehiculoActualizado.getPlaca())) {
                if (!editor.getRol().equals(Rol.ADMIN)) {
                    throw new SecurityException("Solo un administrador puede cambiar la placa de un vehículo.");
                }
                if (vehiculoRepository.findByPlaca(vehiculoActualizado.getPlaca()).isPresent()) {
                    throw new IllegalArgumentException("La nueva placa ya está registrada en otro vehículo.");
                }
                vehiculoExistente.setPlaca(vehiculoActualizado.getPlaca());
            }

            vehiculoExistente.setCapacidadKg(vehiculoActualizado.getCapacidadKg());
            vehiculoExistente.setModelo(vehiculoActualizado.getModelo());

            Vehiculo vehiculoGuardado = vehiculoRepository.save(vehiculoExistente);
            registrarAuditoria(vehiculoGuardado, TipoOperacion.ACTUALIZAR, editor);
            return vehiculoGuardado;
        }).orElseThrow(() -> new ResourceNotFoundException("Vehiculo no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminarVehiculo(Long id, String usernameEditor) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo no encontrado con ID: " + id));
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        try {
            vehiculoRepository.delete(vehiculo);
            registrarAuditoria(vehiculo, TipoOperacion.ELIMINAR, editor);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el vehículo con ID " + id + " porque tiene registros relacionados. " +
                                            "Considere inactivarlo en su lugar o desvincularlo primero.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Error al intentar eliminar el vehículo con ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Vehiculo cambiarEstadoActivoVehiculo(Long id, Boolean nuevoEstado, String usernameEditor) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo no encontrado con ID: " + id));
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);
        
        if (vehiculo.getActivo() != nuevoEstado) {
            int updatedRows = vehiculoRepository.updateActivoStatus(id, nuevoEstado);
            if (updatedRows > 0) {
                vehiculo.setActivo(nuevoEstado);
                registrarAuditoria(vehiculo, TipoOperacion.ACTUALIZAR, editor);
                return vehiculo;
            } else {
                throw new IllegalStateException("No se pudo actualizar el estado del vehículo con ID: " + id);
            }
        }
        return vehiculo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehiculo> obtenerVehiculosActivos() {
        return vehiculoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehiculo> obtenerVehiculosInactivos() {
        return vehiculoRepository.findByActivoFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoAudit> obtenerHistorialCambiosPorVehiculo(Long vehiculoId) {
        return vehiculoAuditRepository.findByVehiculoIdOrderByFechaCambioDesc(vehiculoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoAudit> obtenerHistorialCambiosPorPlaca(String placa) {
        // En el servicio, este método busca el historial directamente por placa.
        // Si no hay historial para la placa, simplemente devuelve una lista vacía.
        return vehiculoAuditRepository.findByVehiculoPlacaOrderByFechaCambioDesc(placa);
    }
}
