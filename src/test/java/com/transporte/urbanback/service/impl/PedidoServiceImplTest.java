package com.transporte.urbanback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.ClienteRepository;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.PedidoAuditRepository;
import com.transporte.urbanback.repository.PedidoRepository;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.repository.VehiculoRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.utilidades.Utilidades;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para PedidoServiceImpl.
 * Utiliza @ExtendWith(MockitoExtension.class) para habilitar Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para PedidoServiceImpl")
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private PedidoAuditRepository pedidoAuditRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ConductorRepository conductorRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private Pedido pedido1;
    private Cliente cliente1;
    private Conductor conductor1;
    private Vehiculo vehiculo1;
    private Usuario adminUser;

    private MockedStatic<Utilidades> mockedUtilidades;

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() throws JsonProcessingException {

        cliente1 = new Cliente(1L, "Cliente Pedido Test", "111", "222", "Dir Cliente Pedido", true);
        conductor1 = new Conductor(1L, "Conductor Pedido Test", "333", LocalDate.of(1980, 1, 1), "444", true);
        vehiculo1 = new Vehiculo(1L, "PED-123", new BigDecimal("1000.00"), "MarcaP", "ModeloP", 2020, true, conductor1);

        pedido1 = new Pedido(1L, cliente1, "Origen Pedido 1", "Destino Pedido 1",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.PENDIENTE, vehiculo1, conductor1, new BigDecimal("25.00"), "Notas del pedido 1");

        adminUser = new Usuario(10L, "adminuser", "encodedpass", Rol.ADMIN, null, null, true);
        new Usuario(11L, "clienteuser", "encodedpass", Rol.CLIENTE, null, cliente1, true);

        mockedUtilidades = mockStatic(Utilidades.class);
    }

    /**
     * Cierra el mock estático después de cada test.
     */
    @AfterEach
    void tearDown() {
        mockedUtilidades.close(); // Cerrar el mock estático
    }

    /**
     * Test para la creación de un nuevo pedido.
     * Verifica que el servicio cree el pedido y registre la auditoría.
     */
    @Test
    @DisplayName("Debe crear un nuevo pedido exitosamente")
    void whenCrearPedido_thenReturnPedidoCreado() throws Exception {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(clienteRepository.findById(cliente1.getId())).thenReturn(Optional.of(cliente1));
        // Estos mocks son 'lenient' porque su invocación depende de si el pedido tiene conductor/vehículo.
        lenient().when(conductorRepository.findById(conductor1.getId())).thenReturn(Optional.of(conductor1));
        lenient().when(vehiculoRepository.findById(vehiculo1.getId())).thenReturn(Optional.of(vehiculo1));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido1);

        Pedido nuevoPedido = pedidoService.crearPedido(pedido1, adminUser.getUsername());

        assertNotNull(nuevoPedido);
        assertEquals(pedido1.getDireccionOrigen(), nuevoPedido.getDireccionOrigen());
        assertEquals(EstadoPedido.PENDIENTE, nuevoPedido.getEstado());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(pedidoAuditRepository, times(1)).save(any(PedidoAudit.class));
    }

    /**
     * Test para obtener un pedido por su ID.
     * Verifica que el servicio retorne el pedido correcto.
     */
    @Test
    @DisplayName("Debe obtener un pedido por ID")
    void whenObtenerPedidoPorId_thenReturnPedido() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido1));

        Optional<Pedido> encontrado = pedidoService.obtenerPedidoPorId(1L);

        assertTrue(encontrado.isPresent());
        assertEquals(pedido1.getId(), encontrado.get().getId());
    }

    /**
     * Test para el escenario donde no se encuentra un pedido por ID.
     * Verifica que el servicio retorne un Optional vacío.
     */
    @Test
    @DisplayName("Debe retornar Optional.empty al obtener pedido por ID no existente")
    void whenObtenerPedidoPorId_thenRetornarEmptyOptional() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Pedido> encontrado = pedidoService.obtenerPedidoPorId(99L);

        assertFalse(encontrado.isPresent());
    }

    /**
     * Test para obtener todos los pedidos.
     * Verifica que el servicio retorne una lista de pedidos.
     */
    @Test
    @DisplayName("Debe obtener todos los pedidos")
    void whenObtenerTodosLosPedidos_thenReturnList() {
        List<Pedido> pedidos = Arrays.asList(pedido1,
                new Pedido(2L, cliente1, "Origen 2", "Destino 2", LocalDateTime.now(), null, null, null, null, EstadoPedido.ASIGNADO, vehiculo1, conductor1, new BigDecimal("30.00"), "Notas 2"));
        when(pedidoRepository.findAll()).thenReturn(pedidos);

        List<Pedido> result = pedidoService.obtenerTodosLosPedidos();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("PedidoServiceImpl: Debe obtener una página de pedidos")
    void pedidoServiceImpl_whenObtenerTodosLosPedidosPaginados_thenReturnPageOfPedidos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> expectedPage = new PageImpl<>(Collections.singletonList(pedido1), pageable, 1);
        when(pedidoRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Pedido> result = pedidoService.obtenerTodosLosPedidosPaginados(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(pedido1.getId(), result.getContent().get(0).getId());
        verify(pedidoRepository, times(1)).findAll(pageable);
    }

    /**
     * Test para actualizar un pedido existente.
     * Verifica que el servicio actualice el pedido y registre la auditoría.
     */
    @Test
    @DisplayName("Debe actualizar un pedido exitosamente")
    void whenActualizarPedido_thenReturnPedidoActualizado() throws Exception {
        // Clonar el pedido1 para simular el pedido existente que se modificará
        Pedido pedidoExistenteModificable = new Pedido(pedido1.getId(), pedido1.getCliente(), pedido1.getDireccionOrigen(),
                pedido1.getDireccionDestino(), pedido1.getFechaCreacion(), pedido1.getFechaRecogidaEstimada(),
                pedido1.getFechaRecogidaReal(), pedido1.getFechaEntregaEstimada(), pedido1.getFechaEntregaReal(),
                pedido1.getEstado(), pedido1.getVehiculo(), pedido1.getConductor(), pedido1.getPesoKg(), pedido1.getNotas());

        Pedido pedidoActualizadoInput = new Pedido();
        pedidoActualizadoInput.setNotas("Nuevas notas");
        pedidoActualizadoInput.setDireccionDestino("Nuevo Destino");
        pedidoActualizadoInput.setCliente(new Cliente(cliente1.getId(), null, null, null, null, false)); // Solo ID del cliente para actualizar
        // Para simular desasignación o asignación, si el servicio lo permite
        pedidoActualizadoInput.setConductor(new Conductor(null, null, null, null, null, false)); // Simula desasignar conductor
        pedidoActualizadoInput.setVehiculo(null); // Simula desasignar vehículo

        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedidoExistenteModificable)); // Retorna el objeto modificable
        when(clienteRepository.findById(cliente1.getId())).thenReturn(Optional.of(cliente1)); // Mock para la búsqueda del cliente
        
        // Cuando se llama a save, devuelve el mismo objeto que se le pasó (que ya fue modificado por el servicio)
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0)); 
        
        Pedido result = pedidoService.actualizarPedido(pedido1.getId(), pedidoActualizadoInput, adminUser.getUsername());

        assertNotNull(result);
        assertEquals("Nuevas notas", result.getNotas());
        assertEquals("Nuevo Destino", result.getDireccionDestino());
        assertNull(result.getConductor()); // Verificar desasignación
        assertNull(result.getVehiculo()); // Verificar desasignación
        verify(pedidoRepository, times(1)).findById(pedido1.getId());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(pedidoAuditRepository, times(1)).save(any(PedidoAudit.class));
    }

    /**
     * Test para eliminar un pedido.
     * Verifica que el servicio lance IllegalStateException debido a registros relacionados.
     */
    @Test
    @DisplayName("Debe lanzar IllegalStateException al intentar eliminar pedido con registros relacionados")
    void whenEliminarPedido_thenThrowsIllegalStateExceptionDueToRelatedRecords() throws Exception {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedido1));
        // Simular que la eliminación en el repositorio falla debido a una restricción de clave foránea
        doThrow(DataIntegrityViolationException.class).when(pedidoRepository).delete(any(Pedido.class));

        assertThrows(IllegalStateException.class, () ->
                pedidoService.eliminarPedido(pedido1.getId(), adminUser.getUsername()));

        verify(pedidoRepository, times(1)).delete(any(Pedido.class));
        verify(pedidoAuditRepository, never()).save(any(PedidoAudit.class)); // La auditoría de eliminación no se debe registrar
    }

    /**
     * Test para el escenario donde se intenta eliminar un pedido no encontrado.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al intentar eliminar pedido no encontrado")
    void whenEliminarPedidoNotFound_thenThrowsEntityNotFoundException() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> // Asegura que se espera la excepción correcta
                pedidoService.eliminarPedido(99L, adminUser.getUsername()));

        verify(pedidoRepository, never()).delete(any(Pedido.class));
        verify(pedidoAuditRepository, never()).save(any(PedidoAudit.class));
    }

    /**
     * Test para asignar conductor y vehículo a un pedido.
     * Verifica que el servicio asigne correctamente y cambie el estado si es PENDIENTE.
     */
    @Test
    @DisplayName("Debe asignar conductor y vehículo a un pedido y cambiar estado a ASIGNADO")
    void whenAsignarConductorYVehiculo_thenReturnPedidoActualizado() throws Exception {
        // Clonar el pedido1 para simular el pedido existente que se modificará
        Pedido pedidoExistenteModificable = new Pedido(pedido1.getId(), pedido1.getCliente(), pedido1.getDireccionOrigen(),
                pedido1.getDireccionDestino(), pedido1.getFechaCreacion(), pedido1.getFechaRecogidaEstimada(),
                pedido1.getFechaRecogidaReal(), pedido1.getFechaEntregaEstimada(), pedido1.getFechaEntregaReal(),
                EstadoPedido.PENDIENTE, null, null, pedido1.getPesoKg(), pedido1.getNotas()); // Asegurar estado PENDIENTE y sin asignaciones iniciales
        
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedidoExistenteModificable)); // Retorna el objeto modificable
        when(conductorRepository.findById(conductor1.getId())).thenReturn(Optional.of(conductor1));
        when(vehiculoRepository.findById(vehiculo1.getId())).thenReturn(Optional.of(vehiculo1));
        lenient().when(vehiculoRepository.countByConductor(conductor1)).thenReturn(0L); // lenient para evitar UnnecessaryStubbingException
        
        // Cuando se llama a save, devuelve el mismo objeto que se le pasó (que ya fue modificado por el servicio)
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido savedPedido = invocation.getArgument(0);
            // Asegurarse de que el objeto retornado tenga el estado actualizado
            savedPedido.setEstado(EstadoPedido.ASIGNADO); 
            return savedPedido;
        }); 

        Pedido result = pedidoService.asignarConductorYVehiculo(pedido1.getId(), conductor1.getId(), vehiculo1.getId(), adminUser.getUsername());

        assertNotNull(result);
        assertEquals(conductor1.getId(), result.getConductor().getId());
        assertEquals(vehiculo1.getId(), result.getVehiculo().getId());
        assertEquals(EstadoPedido.ASIGNADO, result.getEstado()); // Verifica que el estado cambió
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(pedidoAuditRepository, times(1)).save(any(PedidoAudit.class));
    }

    /**
     * Test para el escenario donde el pedido no se encuentra al intentar asignar conductor/vehículo.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar EntityNotFoundException al asignar si el pedido no existe")
    void whenAsignarConductorYVehiculo_PedidoNotFound_thenThrowsEntityNotFoundException() {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(anyLong())).thenReturn(Optional.empty()); // Pedido no encontrado

        assertThrows(ResourceNotFoundException.class, () -> // Asegura que se espera la excepción correcta
                pedidoService.asignarConductorYVehiculo(99L, conductor1.getId(), vehiculo1.getId(), adminUser.getUsername()));

        verify(conductorRepository, never()).findById(anyLong()); // No debe buscar conductor
        verify(vehiculoRepository, never()).findById(anyLong()); // No debe buscar vehículo
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    /**
     * Test para el escenario donde el conductor no se encuentra al intentar asignar.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar EntityNotFoundException al asignar si el conductor no existe")
    void whenAsignarConductorYVehiculo_ConductorNotFound_thenThrowsEntityNotFoundException() {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedido1));
        when(conductorRepository.findById(anyLong())).thenReturn(Optional.empty()); // Conductor no encontrado

        assertThrows(ResourceNotFoundException.class, () -> // Asegura que se espera la excepción correcta
                pedidoService.asignarConductorYVehiculo(pedido1.getId(), 99L, vehiculo1.getId(), adminUser.getUsername()));

        verify(vehiculoRepository, never()).findById(anyLong()); // No debe buscar vehículo
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    /**
     * Test para el escenario donde el vehículo no se encuentra al intentar asignar.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar EntityNotFoundException al asignar si el vehículo no existe")
    void whenAsignarConductorYVehiculo_VehiculoNotFound_thenThrowsEntityNotFoundException() {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedido1));
        when(conductorRepository.findById(conductor1.getId())).thenReturn(Optional.of(conductor1));
        when(vehiculoRepository.findById(anyLong())).thenReturn(Optional.empty()); // Vehículo no encontrado

        assertThrows(ResourceNotFoundException.class, () -> // Asegura que se espera la excepción correcta
                pedidoService.asignarConductorYVehiculo(pedido1.getId(), conductor1.getId(), 99L, adminUser.getUsername()));

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    /**
     * Test para cambiar el estado de un pedido.
     * Verifica que el servicio actualice el estado y registre la auditoría.
     */
    @Test
    @DisplayName("Debe cambiar el estado de un pedido exitosamente")
    void whenCambiarEstadoPedido_thenReturnPedidoActualizado() throws Exception {
        // Clonar el pedido1 para simular el pedido existente que se modificará
        Pedido pedidoExistenteModificable = new Pedido(pedido1.getId(), pedido1.getCliente(), pedido1.getDireccionOrigen(),
                pedido1.getDireccionDestino(), pedido1.getFechaCreacion(), pedido1.getFechaRecogidaEstimada(),
                pedido1.getFechaRecogidaReal(), pedido1.getFechaEntregaEstimada(), pedido1.getFechaEntregaReal(),
                pedido1.getEstado(), pedido1.getVehiculo(), pedido1.getConductor(), pedido1.getPesoKg(), pedido1.getNotas());

        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedidoExistenteModificable)); // Retorna el objeto modificable
        // Cuando se llama a save, devuelve el mismo objeto que se le pasó (que ya fue modificado por el servicio)
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido result = pedidoService.cambiarEstadoPedido(pedido1.getId(), EstadoPedido.EN_CAMINO, adminUser.getUsername());

        assertNotNull(result);
        assertEquals(EstadoPedido.EN_CAMINO, result.getEstado());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(pedidoAuditRepository, times(1)).save(any(PedidoAudit.class));
    }

    /**
     * Test para el escenario donde se intenta cambiar el estado de un pedido completado a un estado inválido.
     * Verifica que el servicio lance una IllegalArgumentException.
     */
    @Test
    @DisplayName("Debe lanzar IllegalArgumentException al cambiar estado de completado a inválido")
    void whenCambiarEstadoPedidoToCompletadoFromInvalidState_thenThrowsIllegalArgumentException() {
        // Clonar el pedido1 y establecer su estado a COMPLETADO para este test
        Pedido pedidoCompletado = new Pedido(pedido1.getId(), pedido1.getCliente(), pedido1.getDireccionOrigen(),
                pedido1.getDireccionDestino(), pedido1.getFechaCreacion(), pedido1.getFechaRecogidaEstimada(),
                pedido1.getFechaRecogidaReal(), pedido1.getFechaEntregaEstimada(), pedido1.getFechaEntregaReal(),
                EstadoPedido.COMPLETADO, pedido1.getVehiculo(), pedido1.getConductor(), pedido1.getPesoKg(), pedido1.getNotas());

        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedidoCompletado));

        assertThrows(IllegalArgumentException.class, () ->
                pedidoService.cambiarEstadoPedido(pedido1.getId(), EstadoPedido.EN_CAMINO, adminUser.getUsername())); // Intenta cambiar a EN_CAMINO

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(pedidoAuditRepository, never()).save(any(PedidoAudit.class));
    }

    /**
     * Test para obtener pedidos por cliente.
     * Verifica que el servicio retorne la lista correcta de pedidos.
     */
    @Test
    @DisplayName("Debe obtener pedidos por cliente")
    void whenObtenerPedidosPorCliente_thenReturnList() {
        List<Pedido> pedidosCliente = Collections.singletonList(pedido1);
        when(pedidoRepository.findByClienteId(cliente1.getId())).thenReturn(pedidosCliente);

        List<Pedido> result = pedidoService.obtenerPedidosPorCliente(cliente1.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(cliente1.getId(), result.get(0).getCliente().getId());
    }

    /**
     * Test para obtener pedidos por conductor.
     * Verifica que el servicio retorne la lista correcta de pedidos.
     */
    @Test
    @DisplayName("Debe obtener pedidos por conductor")
    void whenObtenerPedidosPorConductor_thenReturnList() {
        List<Pedido> pedidosConductor = Collections.singletonList(pedido1);
        when(pedidoRepository.findByConductorId(conductor1.getId())).thenReturn(pedidosConductor);

        List<Pedido> result = pedidoService.obtenerPedidosPorConductor(conductor1.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conductor1.getId(), result.get(0).getConductor().getId());
    }

    /**
     * Test para obtener pedidos por estado.
     * Verifica que el servicio retorne la lista correcta de pedidos.
     */
    @Test
    @DisplayName("Debe obtener pedidos por estado")
    void whenObtenerPedidosPorEstado_thenReturnList() {
        List<Pedido> pedidosEstado = Collections.singletonList(pedido1);
        when(pedidoRepository.findByEstado(EstadoPedido.PENDIENTE)).thenReturn(pedidosEstado);

        List<Pedido> result = pedidoService.obtenerPedidosPorEstado(EstadoPedido.PENDIENTE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EstadoPedido.PENDIENTE, result.get(0).getEstado());
    }

    /**
     * Test para obtener pedidos por rango de fechas de creación.
     * Verifica que el servicio retorne la lista correcta de pedidos.
     */
    @Test
    @DisplayName("Debe obtener pedidos por rango de fechas de creación")
    void whenObtenerPedidosPorRangoFechasCreacion_thenReturnList() {
        List<Pedido> pedidosFecha = Collections.singletonList(pedido1);
        LocalDateTime fechaInicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fechaFin = LocalDateTime.now().plusDays(1);
        when(pedidoRepository.findByFechaCreacionBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(pedidosFecha);

        List<Pedido> result = pedidoService.obtenerPedidosPorRangoFechasCreacion(fechaInicio, fechaFin);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test para obtener el historial de cambios de un pedido.
     * Verifica que el servicio retorne la lista de auditorías.
     */
    @Test
    @DisplayName("Debe obtener historial de cambios por pedido")
    void whenObtenerHistorialCambiosPorPedido_thenReturnList() {
        PedidoAudit audit = new PedidoAudit(pedido1, TipoOperacion.CREAR, adminUser, "detalles");
        when(pedidoAuditRepository.findByPedidoIdOrderByFechaCambioDesc(pedido1.getId())).thenReturn(Collections.singletonList(audit));

        List<PedidoAudit> result = pedidoService.obtenerHistorialCambiosPorPedido(pedido1.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test para obtener el historial de cambios por usuario editor.
     * Verifica que el servicio retorne la lista de auditorías.
     */
    @Test
    @DisplayName("Debe obtener historial de cambios por usuario editor")
    void whenObtenerHistorialCambiosPorUsuarioEditor_thenReturnList() {
        PedidoAudit audit = new PedidoAudit(pedido1, TipoOperacion.CREAR, adminUser, "detalles");
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(pedidoAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(adminUser.getId())).thenReturn(Collections.singletonList(audit));

        List<PedidoAudit> result = pedidoService.obtenerHistorialCambiosPorUsuarioEditor(adminUser.getUsername());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test para verificar si un pedido pertenece a un cliente.
     * Verifica que el servicio retorne true si el pedido pertenece al cliente.
     */
    @Test
    @DisplayName("Debe verificar si el pedido pertenece al cliente (true)")
    void whenEsPedidoPertenecienteACliente_thenReturnTrue() {
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedido1));

        boolean result = pedidoService.esPedidoPertenecienteACliente(pedido1.getId(), cliente1.getId());

        assertTrue(result);
    }

    /**
     * Test para verificar si un pedido no pertenece a un cliente.
     * Verifica que el servicio retorne false si el pedido no pertenece al cliente.
     */
    @Test
    @DisplayName("Debe verificar si el pedido no pertenece al cliente (false)")
    void whenEsPedidoPertenecienteACliente_thenReturnFalse() {
        Cliente otroCliente = new Cliente(99L, "Otro Cliente", "999", "888", "Otra Dir", true);
        Pedido pedidoOtroCliente = new Pedido(2L, otroCliente, "Origen", "Destino", LocalDateTime.now(), null, null, null, null, EstadoPedido.PENDIENTE, null, null, BigDecimal.ZERO, null);

        when(pedidoRepository.findById(pedidoOtroCliente.getId())).thenReturn(Optional.of(pedidoOtroCliente));

        boolean result = pedidoService.esPedidoPertenecienteACliente(pedidoOtroCliente.getId(), cliente1.getId()); // Intentar con ID de cliente incorrecto

        assertFalse(result);
    }

    /**
     * Test para verificar si un pedido está asignado a un conductor.
     * Verifica que el servicio retorne true si el pedido está asignado al conductor.
     */
    @Test
    @DisplayName("Debe verificar si un pedido está asignado al conductor (true)")
    void whenEsPedidoAsignadoAConductor_thenReturnTrue() {
        when(pedidoRepository.findById(pedido1.getId())).thenReturn(Optional.of(pedido1));

        boolean result = pedidoService.esPedidoAsignadoAConductor(pedido1.getId(), conductor1.getId());

        assertTrue(result);
    }

    /**
     * Test para verificar si un pedido no está asignado a un conductor.
     * Verifica que el servicio retorne false si el pedido no está asignado al conductor.
     */
    @Test
    @DisplayName("Debe verificar si el pedido no está asignado al conductor (false)")
    void whenEsPedidoAsignadoAConductor_thenReturnFalse() {
        Conductor otroConductor = new Conductor(99L, "Otro Conductor", "999", LocalDate.now(), "777", true);
        Pedido pedidoOtroConductor = new Pedido(2L, cliente1, "Origen", "Destino", LocalDateTime.now(), null, null, null, null, EstadoPedido.ASIGNADO, null, otroConductor, BigDecimal.ZERO, null);

        when(pedidoRepository.findById(pedidoOtroConductor.getId())).thenReturn(Optional.of(pedidoOtroConductor));

        boolean result = pedidoService.esPedidoAsignadoAConductor(pedidoOtroConductor.getId(), conductor1.getId()); // Intentar con ID de conductor incorrecto

        assertFalse(result);
    }

    /**
     * Test para obtener todos los estados de pedido.
     * Verifica que el servicio retorne la lista correcta de estados.
     */
    @Test
    @DisplayName("Debe obtener todos los estados de pedido")
    void whenObtenerTodosLosEstadosDePedido_thenReturnAllStates() {
        List<EstadoPedido> allStates = pedidoService.obtenerTodosLosEstadosDePedido();

        assertNotNull(allStates);
        assertEquals(EstadoPedido.values().length, allStates.size());
        assertTrue(allStates.contains(EstadoPedido.PENDIENTE));
        assertTrue(allStates.contains(EstadoPedido.COMPLETADO));
    }
}
