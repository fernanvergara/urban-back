package com.transporte.urbanback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transporte.urbanback.constants.Constantes;
import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.ClienteRepository;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.PedidoAuditRepository;
import com.transporte.urbanback.repository.PedidoRepository;
import com.transporte.urbanback.repository.VehiculoRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.PedidoService;
import com.transporte.urbanback.utilidades.Utilidades;

import lombok.extern.slf4j.Slf4j;

import com.transporte.urbanback.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoAuditRepository pedidoAuditRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PedidoAuditRepository pedidoAuditRepository,
                             ClienteRepository clienteRepository,
                             ConductorRepository conductorRepository,
                             VehiculoRepository vehiculoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoAuditRepository = pedidoAuditRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private void registrarAuditoria(Pedido pedido, TipoOperacion tipoOperacion, Usuario usuarioEditor) {
        if (pedido == null) {
            log.error("Advertencia: Intento de registrar auditoría para un pedido nulo. Operación ignorada.");
            return;
        }

        String detallesCambio = "";
        if (tipoOperacion != TipoOperacion.ELIMINAR) {
            try {
                detallesCambio = objectMapper.writeValueAsString(pedido);
            } catch (JsonProcessingException e) {
                log.error("Error al serializar Pedido para auditoría: " + e.getMessage());
                detallesCambio = "{ \"error\": \"No se pudo serializar el objeto\" }";
            }
        } else {
            detallesCambio = "{}";
        }

        PedidoAudit audit = new PedidoAudit(pedido, tipoOperacion, usuarioEditor, detallesCambio);
        pedidoAuditRepository.save(audit);
    }

    @Override
    @Transactional
    public Pedido crearPedido(Pedido pedido, String usernameEditor) {
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);

        Cliente cliente = clienteRepository.findById(pedido.getCliente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + pedido.getCliente().getId())); // CAMBIADO
        pedido.setCliente(cliente);

        if (pedido.getConductor() != null && pedido.getConductor().getId() != null) {
            Conductor conductor = conductorRepository.findById(pedido.getConductor().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + pedido.getConductor().getId())); // CAMBIADO
            pedido.setConductor(conductor);
        }
        if (pedido.getVehiculo() != null && pedido.getVehiculo().getId() != null) {
            Vehiculo vehiculo = vehiculoRepository.findById(pedido.getVehiculo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + pedido.getVehiculo().getId())); // CAMBIADO
            pedido.setVehiculo(vehiculo);
        }

        if (pedido.getFechaCreacion() == null) {
            pedido.setFechaCreacion(LocalDateTime.now());
        }
        if (pedido.getEstado() == null || pedido.getEstado().name().isEmpty()) {
            pedido.setEstado(EstadoPedido.PENDIENTE); 
        }

        Pedido savedPedido = pedidoRepository.save(pedido);
        registrarAuditoria(savedPedido, TipoOperacion.CREAR, usuarioEditor);
        return savedPedido;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pedido> obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Pedido> obtenerTodosLosPedidosPaginados(Pageable pageable) {
        return pedidoRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Pedido actualizarPedido(Long id, Pedido pedidoActualizado, String usernameEditor) {
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);
        Pedido pedidoExistente = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id)); // CAMBIADO

        if (pedidoActualizado.getNotas() != null) {
            pedidoExistente.setNotas(pedidoActualizado.getNotas());
        }
        if (pedidoActualizado.getDireccionOrigen() != null) {
            pedidoExistente.setDireccionOrigen(pedidoActualizado.getDireccionOrigen());
        }
        if (pedidoActualizado.getDireccionDestino() != null) {
            pedidoExistente.setDireccionDestino(pedidoActualizado.getDireccionDestino());
        }
        if (pedidoActualizado.getFechaEntregaEstimada() != null) {
            pedidoExistente.setFechaEntregaEstimada(pedidoActualizado.getFechaEntregaEstimada());
        }

        if (pedidoActualizado.getCliente() != null && pedidoActualizado.getCliente().getId() != null) {
            Cliente nuevoCliente = clienteRepository.findById(pedidoActualizado.getCliente().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + pedidoActualizado.getCliente().getId())); // CAMBIADO
            pedidoExistente.setCliente(nuevoCliente);
        }

        if (pedidoActualizado.getConductor() != null) {
            if (pedidoActualizado.getConductor().getId() == null) {
                pedidoExistente.setConductor(null);
            } else {
                Conductor nuevoConductor = conductorRepository.findById(pedidoActualizado.getConductor().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + pedidoActualizado.getConductor().getId())); // CAMBIADO
                pedidoExistente.setConductor(nuevoConductor);
            }
        } else if (pedidoActualizado.getConductor() == null && pedidoExistente.getConductor() != null) {
             pedidoExistente.setConductor(null);
        }

        if (pedidoActualizado.getVehiculo() != null) {
            if (pedidoActualizado.getVehiculo().getId() == null) {
                pedidoExistente.setVehiculo(null);
            } else {
                Vehiculo nuevoVehiculo = vehiculoRepository.findById(pedidoActualizado.getVehiculo().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + pedidoActualizado.getVehiculo().getId())); // CAMBIADO
                pedidoExistente.setVehiculo(nuevoVehiculo);
            }
        } else if (pedidoActualizado.getVehiculo() == null && pedidoExistente.getVehiculo() != null) {
            pedidoExistente.setVehiculo(null);
        }

        Pedido updatedPedido = pedidoRepository.save(pedidoExistente);
        registrarAuditoria(updatedPedido, TipoOperacion.ACTUALIZAR, usuarioEditor);
        return updatedPedido;
    }

    @Override
    @Transactional
    public void eliminarPedido(Long id, String usernameEditor) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id)); // CAMBIADO
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);

        try {
            pedidoRepository.delete(pedido);
            registrarAuditoria(pedido, TipoOperacion.ELIMINAR, usuarioEditor);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el pedido con ID " + id + " debido a una restricción de integridad de datos. " +
                                            "Asegúrese de que no haya dependencias (ej. registros de pago, historial de viaje) antes de eliminarlo.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Error al intentar eliminar el pedido con ID " + id + ": " + e.getMessage(), e); // CAMBIADO a IllegalStateException
        }
    }

    @Override
    @Transactional
    public Pedido asignarConductorYVehiculo(Long pedidoId, Long conductorId, Long vehiculoId, String usernameEditor) {
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + pedidoId)); // CAMBIADO
        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + conductorId)); // CAMBIADO
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + vehiculoId)); // CAMBIADO

        if (!conductor.getActivo()) {
            throw new IllegalArgumentException("El conductor con ID " + conductorId + " no está activo.");
        }
        if (!vehiculo.getActivo()) {
            throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " no está activo.");
        }
        
        if (vehiculo.getConductor() != null && !vehiculo.getConductor().getId().equals(conductorId)) {
             throw new IllegalArgumentException("El vehículo con ID " + vehiculoId + " ya está asignado a otro conductor.");
        }

        if (vehiculoRepository.countByConductor(conductor) >= Constantes.MAX_VEHICULOS_POR_CONDUCTOR) {
            throw new IllegalStateException("El conductor con ID " + conductorId + " ya tiene " + Constantes.MAX_VEHICULOS_POR_CONDUCTOR + " vehículos asignados y no puede tomar más.");
        }

        pedido.setConductor(conductor);
        pedido.setVehiculo(vehiculo);
        
        if ("PENDIENTE".equals(pedido.getEstado())) {
            pedido.setEstado(EstadoPedido.ASIGNADO); 
        }

        Pedido updatedPedido = pedidoRepository.save(pedido);
        registrarAuditoria(updatedPedido, TipoOperacion.ACTUALIZAR, usuarioEditor);
        return updatedPedido;
    }

    @Override
    @Transactional
    public Pedido cambiarEstadoPedido(Long pedidoId, EstadoPedido nuevoEstado, String usernameEditor) {
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + pedidoId)); // CAMBIADO

        if (EstadoPedido.COMPLETADO.equals(pedido.getEstado()) && !nuevoEstado.equals(EstadoPedido.CANCELADO)) { // Corregido: comparar con enum
            throw new IllegalArgumentException("No se puede cambiar el estado de un pedido completado a " + nuevoEstado + " (solo CANCELADO).");
        }
        
        pedido.setEstado(nuevoEstado);
        Pedido updatedPedido = pedidoRepository.save(pedido);
        registrarAuditoria(updatedPedido, TipoOperacion.ACTUALIZAR, usuarioEditor);
        return updatedPedido;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorConductor(Long conductorId) {
        return pedidoRepository.findByConductorId(conductorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorRangoFechasCreacion(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pedidoRepository.findByFechaCreacionBetween(fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoAudit> obtenerHistorialCambiosPorPedido(Long pedidoId) {
        return pedidoAuditRepository.findByPedidoIdOrderByFechaCambioDesc(pedidoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoAudit> obtenerHistorialCambiosPorUsuarioEditor(String usernameEditor) {
        Usuario usuarioEditor = Utilidades.getUsuarioEditor(usernameEditor);
        return pedidoAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(usuarioEditor.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorConductorYEstado(Long conductorId, EstadoPedido estado) {
        return pedidoRepository.findByConductorIdAndEstado(conductorId, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorClienteYEstado(Long clienteId, EstadoPedido estado) {
        return pedidoRepository.findByClienteIdAndEstado(clienteId, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esPedidoPertenecienteACliente(Long pedidoId, Long clienteId) {
        return pedidoRepository.findById(pedidoId)
                .map(pedido -> pedido.getCliente() != null && pedido.getCliente().getId().equals(clienteId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esPedidoAsignadoAConductor(Long pedidoId, Long conductorId) {
        return pedidoRepository.findById(pedidoId)
                .map(pedido -> pedido.getConductor() != null && pedido.getConductor().getId().equals(conductorId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoPedido> obtenerTodosLosEstadosDePedido() {
        return Arrays.asList(EstadoPedido.values());
    }
}
