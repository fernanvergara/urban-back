package com.transporte.urbanback.service.impl;

import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.repository.ClienteAuditRepository;
import com.transporte.urbanback.repository.ClienteRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.ClienteService;
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
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteAuditRepository clienteAuditRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              ClienteAuditRepository clienteAuditRepository,
                              ObjectMapper objectMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteAuditRepository = clienteAuditRepository;
        this.objectMapper = objectMapper;
    }

    // Helper para registrar la auditoría
    private void registrarAuditoria(Cliente cliente, TipoOperacion tipoOperacion, Usuario usuarioEditor) {
        if (cliente == null) {
            log.error("Advertencia: Intento de registrar auditoría para un cliente nulo. Operación ignorada.");
            return;
        }

        String detallesCambio = "";
        if (tipoOperacion != TipoOperacion.ELIMINAR) {
            try {
                // Serializar el objeto Conductor a JSON.
                detallesCambio = objectMapper.writeValueAsString(cliente);
            } catch (Exception e) {
                log.error("Error al serializar Cliente para auditoría: " + e.getMessage());
                detallesCambio = "{ \"error\": \"No se pudo serializar el objeto\" }";
            }
        } else {
            detallesCambio = "{}"; 
        }

        ClienteAudit audit = new ClienteAudit(cliente, tipoOperacion, usuarioEditor, detallesCambio);
        clienteAuditRepository.save(audit);
    }

    @Override
    @Transactional
    public Cliente crearCliente(Cliente cliente, String usernameEditor) {
        if (clienteRepository.findByIdentificacion(cliente.getIdentificacion()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente con la identificación: " + cliente.getIdentificacion());
        }

        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);
        Cliente nuevoCliente = clienteRepository.save(cliente);
        
        registrarAuditoria(nuevoCliente, TipoOperacion.CREAR, editor); 

        return nuevoCliente;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerClientePorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> obtenerTodosLosClientes() {
        return clienteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cliente> obtenerTodosLosClientesPaginados(Pageable pageable){
        return clienteRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado, String usernameEditor) {
        return clienteRepository.findById(id).map(clienteExistente -> {
            Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);
            
            if (!clienteExistente.getIdentificacion().equalsIgnoreCase(clienteActualizado.getIdentificacion())) {
                if (clienteRepository.findByIdentificacion(clienteActualizado.getIdentificacion()).isPresent()) {
                    throw new IllegalArgumentException("La nueva identificación ya está registrada en otro cliente.");
                }
                clienteExistente.setIdentificacion(clienteActualizado.getIdentificacion());
            }

            clienteExistente.setNombreCompleto(clienteActualizado.getNombreCompleto());
            clienteExistente.setTelefono(clienteActualizado.getTelefono());
            clienteExistente.setDireccionResidencia(clienteActualizado.getDireccionResidencia());
            clienteExistente.setActivo(clienteActualizado.getActivo());

            Cliente clienteGuardado = clienteRepository.save(clienteExistente);
            
            // Se pasa el objeto actualizado para serializarlo en registrarAuditoria
            registrarAuditoria(clienteGuardado, TipoOperacion.ACTUALIZAR, editor);

            return clienteGuardado;
        }).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id)); 
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id, String usernameEditor) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id)); 
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        try {
            clienteRepository.delete(cliente);
            registrarAuditoria(cliente, TipoOperacion.ELIMINAR, editor); 
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el cliente con ID " + id + " porque tiene registros relacionados (ej. pedidos). " +
                                            "Considere inactivarlo en su lugar o desvincularlo primero.", e);
        } catch (Exception e) {
            // Este catch general es una buena práctica para capturar cualquier otra excepción inesperada
            // y relanzarla como una excepción de estado ilegal o similar, que tu GlobalExceptionHandler pueda manejar.
            throw new IllegalStateException("Error al intentar eliminar el cliente con ID " + id + ": " + e.getMessage(), e); 
        }
    }

    @Override
    @Transactional
    public Cliente cambiarEstadoActivoCliente(Long id, Boolean nuevoEstado, String usernameEditor) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id)); 
        Usuario editor = Utilidades.getUsuarioEditor(usernameEditor);

        if (cliente.getActivo() != nuevoEstado) {
            int updatedRows = clienteRepository.updateActivoStatus(id, nuevoEstado);
            if (updatedRows > 0) {
                cliente.setActivo(nuevoEstado); 
                registrarAuditoria(cliente, TipoOperacion.CAMBIO_ESTADO, editor); 
                return cliente;
            } else {
                throw new IllegalStateException("No se pudo actualizar el estado del cliente con ID: " + id); 
            }
        }
        return cliente;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> obtenerClientesInactivos() {
        return clienteRepository.findByActivoFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteAudit> obtenerHistorialCambiosPorCliente(Long clienteId) {
        return clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteAudit> obtenerHistorialCambiosPorIdentificacion(String identificacion) {
        Optional<Cliente> clienteOpt = clienteRepository.findByIdentificacion(identificacion);
        if (clienteOpt.isEmpty()) {
           throw new ResourceNotFoundException("Identificación no encontrada"); 
        }
         return clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(clienteOpt.get().getId());
    }
}
