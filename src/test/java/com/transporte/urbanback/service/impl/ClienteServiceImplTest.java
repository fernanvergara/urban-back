package com.transporte.urbanback.service.impl;

import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit;
import com.transporte.urbanback.repository.ClienteAuditRepository;
import com.transporte.urbanback.repository.ClienteRepository;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.utilidades.Utilidades;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para ClienteServiceImpl")
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteAuditRepository clienteAuditRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente cliente1;
    private Usuario usuarioEditor;

    private MockedStatic<Utilidades> mockedUtilidades;

    @BeforeEach
    void setUp() {
        cliente1 = new Cliente(1L, "Juan Perez Garcia", "1020304050", "+573001234567", "Calle Falsa 123", true);
        usuarioEditor = new Usuario(1L, "adminuser", "password123", com.transporte.urbanback.enums.Rol.ADMIN, null, null, true);
        mockedUtilidades = mockStatic(Utilidades.class);
    }

    /**
     * Cierra el mock estático después de cada test.
     */
    @AfterEach
    void tearDown() {
        mockedUtilidades.close(); // Cerrar el mock estático
    }

    @Test
    @DisplayName("Debe crear un cliente exitosamente")
    void whenCrearCliente_thenReturnClienteCreado() throws Exception {
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente1);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json_creacion");

        Cliente clienteCreado = clienteService.crearCliente(cliente1, usuarioEditor.getUsername());

        assertNotNull(clienteCreado);
        assertEquals("Juan Perez Garcia", clienteCreado.getNombreCompleto());
        verify(clienteRepository, times(1)).save(cliente1);
        verify(clienteAuditRepository, times(1)).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear cliente con identificación existente")
    void whenCrearClienteWithExistingIdentificacion_thenThrowDataIntegrityViolationException() {
        when(clienteRepository.save(any(Cliente.class))).thenThrow(DataIntegrityViolationException.class);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);

        assertThrows(DataIntegrityViolationException.class, () ->
                clienteService.crearCliente(cliente1, usuarioEditor.getUsername()));

        verify(clienteRepository, times(1)).save(cliente1);
        verify(clienteAuditRepository, never()).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe obtener un cliente por su ID")
    void whenObtenerClientePorId_thenReturnCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente1));

        Cliente encontrado = clienteService.obtenerClientePorId(1L).get();

        assertNotNull(encontrado);
        assertEquals("Juan Perez Garcia", encontrado.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener cliente por ID no existente")
    void whenObtenerClientePorId_thenThrowEntityNotFoundException() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                clienteService.obtenerClientePorId(2L).orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + 2L)));
    }

    @Test
    @DisplayName(" Debe obtener una página de clientes")
    void clienteServiceImpl_whenObtenerTodosLosClientesPaginados_thenReturnPageOfClientes() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> expectedPage = new PageImpl<>(Collections.singletonList(cliente1), pageable, 1);
        when(clienteRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Cliente> result = clienteService.obtenerTodosLosClientesPaginados(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(cliente1.getId(), result.getContent().get(0).getId());
        verify(clienteRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Debe actualizar un cliente exitosamente")
    void whenActualizarCliente_thenReturnClienteActualizado() throws Exception {
        Cliente clienteActualizado = new Cliente(1L, "Juan Carlos Perez", "1020304050", "+573112223344", "Nueva Calle 456", true);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente1));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteActualizado);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json_actualizacion");

        Cliente resultado = clienteService.actualizarCliente(1L, clienteActualizado, usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertEquals("Juan Carlos Perez", resultado.getNombreCompleto());
        assertEquals("Nueva Calle 456", resultado.getDireccionResidencia());
        verify(clienteRepository, times(1)).findById(1L);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(clienteAuditRepository, times(1)).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe eliminar un cliente y registrar auditoría")
    void whenEliminarCliente_thenDeletesAndAudits() throws Exception {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente1));
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);

        clienteService.eliminarCliente(1L, usuarioEditor.getUsername());

        verify(clienteRepository, times(1)).delete(cliente1);
        verify(clienteAuditRepository, times(1)).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar eliminar cliente no encontrado")
    void whenEliminarClienteNotFound_thenThrowsEntityNotFoundException() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                clienteService.eliminarCliente(2L, usuarioEditor.getUsername()));

        verify(clienteRepository, never()).deleteById(anyLong());
        verify(clienteAuditRepository, never()).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe actualizar el estado activo de un cliente")
    void whenCambiarEstadoActivoCliente_thenReturnClienteConEstadoActualizado() throws Exception {
        cliente1.setActivo(true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente1));
        when(clienteRepository.updateActivoStatus(1L, false)).thenReturn(1);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json_estado");

        Cliente resultado = clienteService.cambiarEstadoActivoCliente(1L, false, usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertFalse(resultado.getActivo());
        verify(clienteRepository, times(1)).updateActivoStatus(1L, false);
        verify(clienteAuditRepository, times(1)).save(any(ClienteAudit.class));
    }

    @Test
    @DisplayName("Debe obtener la lista de clientes activos")
    void whenObtenerClientesActivos_thenReturnListaClientesActivos() {
        Cliente activo1 = new Cliente(2L, "Cliente Activo 1", "ID1", "Tel1", "Dir1", true);
        Cliente activo2 = new Cliente(3L, "Cliente Activo 2", "ID2", "Tel2", "Dir2", true);
        when(clienteRepository.findByActivoTrue()).thenReturn(Arrays.asList(activo1, activo2));

        List<Cliente> clientesActivos = clienteService.obtenerClientesActivos();

        assertNotNull(clientesActivos);
        assertEquals(2, clientesActivos.size());
        assertTrue(clientesActivos.stream().allMatch(Cliente::getActivo));
        verify(clienteRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("Debe obtener la lista de clientes inactivos")
    void whenObtenerClientesInactivos_thenReturnListaClientesInactivos() {
        Cliente inactivo1 = new Cliente(4L, "Cliente Inactivo 1", "ID3", "Tel3", "Dir3", false);
        when(clienteRepository.findByActivoFalse()).thenReturn(Arrays.asList(inactivo1));

        List<Cliente> clientesInactivos = clienteService.obtenerClientesInactivos();

        assertNotNull(clientesInactivos);
        assertEquals(1, clientesInactivos.size());
        assertFalse(clientesInactivos.get(0).getActivo());
        verify(clienteRepository, times(1)).findByActivoFalse();
    }

    @Test
    @DisplayName("Debe obtener el historial de cambios de un cliente por su ID")
    void whenObtenerHistorialCambiosPorCliente_thenReturnListaAuditorias() {
        ClienteAudit audit1 = new ClienteAudit(cliente1, TipoOperacion.CREAR, usuarioEditor, "detalles1");
        ClienteAudit audit2 = new ClienteAudit(cliente1, TipoOperacion.ACTUALIZAR, usuarioEditor, "detalles2");
        when(clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(1L)).thenReturn(Arrays.asList(audit1, audit2));

        List<ClienteAudit> historial = clienteService.obtenerHistorialCambiosPorCliente(1L);

        assertNotNull(historial);
        assertEquals(2, historial.size());
        verify(clienteAuditRepository, times(1)).findByClienteIdOrderByFechaCambioDesc(1L);
    }

    @Test
    @DisplayName("Debe obtener el historial de cambios de un cliente por identificación")
    void whenObtenerHistorialCambiosPorIdentificacion_thenReturnListaAuditorias() {
        when(clienteRepository.findByIdentificacion(anyString())).thenReturn(Optional.of(cliente1));
        ClienteAudit audit1 = new ClienteAudit(cliente1, TipoOperacion.CREAR, usuarioEditor, "detalles_id");
        when(clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(anyLong())).thenReturn(Collections.singletonList(audit1));

        List<ClienteAudit> historial = clienteService.obtenerHistorialCambiosPorIdentificacion("1020304050");

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(clienteRepository, times(1)).findByIdentificacion("1020304050");
        verify(clienteAuditRepository, times(1)).findByClienteIdOrderByFechaCambioDesc(cliente1.getId());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener historial por identificación no existente")
    void whenObtenerHistorialCambiosPorIdentificacionNotFound_thenThrowsEntityNotFoundException() {
        when(clienteRepository.findByIdentificacion(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                clienteService.obtenerHistorialCambiosPorIdentificacion("nonexistent"));
        verify(clienteRepository, times(1)).findByIdentificacion("nonexistent");
        verify(clienteAuditRepository, never()).findByClienteIdOrderByFechaCambioDesc(anyLong());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario editor no se encuentra")
    void whenGetUsuarioEditorNotFound_thenThrowsEntityNotFoundException() {
        when(clienteRepository.findByIdentificacion(cliente1.getIdentificacion())).thenReturn(Optional.of(cliente1));
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                clienteService.crearCliente(cliente1, "nonexistent"));
    }
}
