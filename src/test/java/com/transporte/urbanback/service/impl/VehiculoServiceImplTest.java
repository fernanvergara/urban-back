package com.transporte.urbanback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.exception.ResourceNotFoundException;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.repository.VehiculoAuditRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para VehiculoServiceImpl.
 * Utiliza @ExtendWith(MockitoExtension.class) para habilitar Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para VehiculoServiceImpl")
class VehiculoServiceImplTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private VehiculoAuditRepository vehiculoAuditRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConductorRepository conductorRepository; 

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VehiculoServiceImpl vehiculoService;

    private Vehiculo vehiculo1;
    private Conductor conductor1; 
    private Usuario usuarioEditor;
    private Usuario adminUser; 

    private MockedStatic<Utilidades> mockedUtilidades;

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() {
        conductor1 = new Conductor(1L, "Conductor Vehiculo Test", "123456", LocalDate.of(1980, 1, 1), "+573001111111", true);
        
        vehiculo1 = new Vehiculo(1L, "XYZ-789", new BigDecimal("1200.00"), "Toyota", "Corolla", 2020, true, conductor1); 

        usuarioEditor = new Usuario(1L, "editoruser", "password123", Rol.CLIENTE, null, null, true); // Usuario no-ADMIN por defecto
        adminUser = new Usuario(2L, "adminuser", "adminpass", Rol.ADMIN, null, null, true); // Usuario ADMIN

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
     * Test para la creación de un nuevo vehículo.
     * Verifica que el servicio cree el vehículo y registre la auditoría.
     */
    @Test
    @DisplayName("Debe crear un vehículo exitosamente")
    void whenCrearVehiculo_thenReturnVehiculoCreado() throws Exception {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(adminUser.getUsername())).thenReturn(adminUser);
        when(vehiculoRepository.findByPlaca(anyString())).thenReturn(Optional.empty()); // Placa no existe
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculo1);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Vehiculo nuevoVehiculo = vehiculoService.crearVehiculo(vehiculo1, adminUser.getUsername());

        assertNotNull(nuevoVehiculo);
        assertEquals("XYZ-789", nuevoVehiculo.getPlaca());
        verify(vehiculoRepository, times(1)).findByPlaca("XYZ-789"); // Verifica la llamada para chequear existencia
        verify(vehiculoRepository, times(1)).save(vehiculo1);
        verify(vehiculoAuditRepository, times(1)).save(any(VehiculoAudit.class));
    }

    /**
     * Test para el escenario donde se intenta crear un vehículo con una placa ya existente.
     * Verifica que el servicio lance una IllegalArgumentException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al crear vehículo con placa existente")
    void whenCrearVehiculoWithExistingPlaca_thenThrowIllegalArgumentException() {
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        when(vehiculoRepository.findByPlaca(anyString())).thenReturn(Optional.of(vehiculo1)); // Simula que la placa ya existe

        assertThrows(IllegalArgumentException.class, () ->
                vehiculoService.crearVehiculo(vehiculo1, adminUser.getUsername()));

        verify(vehiculoRepository, times(1)).findByPlaca("XYZ-789");
        verify(vehiculoRepository, never()).save(any(Vehiculo.class));
        verify(vehiculoAuditRepository, never()).save(any(VehiculoAudit.class));
    }

    /**
     * Test para obtener un vehículo por su ID.
     * Verifica que el servicio retorne el vehículo correcto.
     */
    @Test
    @DisplayName("Debe obtener un vehículo por su ID")
    void whenObtenerVehiculoPorId_thenReturnVehiculo() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1));

        Vehiculo encontrado = vehiculoService.obtenerVehiculoPorId(1L).get();

        assertNotNull(encontrado);
        assertEquals("XYZ-789", encontrado.getPlaca());
    }

    /**
     * Test para el escenario donde se intenta obtener un vehículo con un ID no existente.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al obtener vehículo por ID no existente")
    void whenObtenerVehiculoPorId_thenThrowEntityNotFoundException() {
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                vehiculoService.obtenerVehiculoPorId(2L).orElseThrow(() -> new EntityNotFoundException("Vehiculo no encontrado con ID: " + 2L)));
    }

    @Test
    @DisplayName("VehiculoServiceImpl: Debe obtener una página de vehículos")
    void vehiculoServiceImpl_whenObtenerTodosLosVehiculosPaginados_thenReturnPageOfVehiculos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehiculo> expectedPage = new PageImpl<>(Collections.singletonList(vehiculo1), pageable, 1);
        when(vehiculoRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Vehiculo> result = vehiculoService.obtenerTodosLosVehiculosPaginados(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(vehiculo1.getId(), result.getContent().get(0).getId());
        verify(vehiculoRepository, times(1)).findAll(pageable);
    }

    /**
     * Test para actualizar un vehículo existente.
     * Verifica que el servicio actualice el vehículo y registre la auditoría.
     */
    @Test
    @DisplayName("Debe actualizar un vehículo exitosamente (sin cambio de placa)")
    void whenActualizarVehiculo_thenReturnVehiculoActualizado() throws Exception {
        Vehiculo vehiculoActualizado = new Vehiculo(1L, "XYZ-789", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, conductor1); 

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1)); 
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculoActualizado);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Vehiculo resultado = vehiculoService.actualizarVehiculo(1L, vehiculoActualizado, adminUser.getUsername());

        assertNotNull(resultado);
        assertEquals("Honda", resultado.getMarca());
        assertEquals(new BigDecimal("1500.00"), resultado.getCapacidadKg());
        verify(vehiculoRepository, times(1)).findById(1L);
        verify(vehiculoRepository, times(1)).save(any(Vehiculo.class));
        verify(vehiculoAuditRepository, times(1)).save(any(VehiculoAudit.class));
        verify(vehiculoRepository, never()).findByPlaca(anyString()); // No se llama findByPlaca si la placa no cambia
    }

    /**
     * Test para actualizar un vehículo con cambio de placa por un ADMIN.
     * Verifica que el servicio actualice la placa y registre la auditoría.
     */
    @Test
    @DisplayName("Debe actualizar un vehículo con cambio de placa por ADMIN")
    void whenActualizarVehiculo_withPlacaChangeByAdmin_thenReturnVehiculoActualizado() throws Exception {
        Vehiculo vehiculoActualizado = new Vehiculo(1L, "NEW-PLACA", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, conductor1); 

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1)); 
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        when(vehiculoRepository.findByPlaca("NEW-PLACA")).thenReturn(Optional.empty()); // Nueva placa no existe
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculoActualizado);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Vehiculo resultado = vehiculoService.actualizarVehiculo(1L, vehiculoActualizado, adminUser.getUsername());

        assertNotNull(resultado);
        assertEquals("NEW-PLACA", resultado.getPlaca());
        verify(vehiculoRepository, times(1)).findByPlaca("NEW-PLACA");
        verify(vehiculoRepository, times(1)).save(any(Vehiculo.class));
        verify(vehiculoAuditRepository, times(1)).save(any(VehiculoAudit.class));
    }

    /**
     * Test para actualizar un vehículo con cambio de placa por un usuario no ADMIN.
     * Verifica que el servicio lance una SecurityException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al actualizar placa por usuario no ADMIN")
    void whenActualizarVehiculo_placaChangeByNonAdmin_thenThrowSecurityException() throws Exception {
        Vehiculo vehiculoActualizado = new Vehiculo(1L, "NEW-PLACA", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, conductor1); 

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1)); 
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);

        assertThrows(SecurityException.class, () ->
                vehiculoService.actualizarVehiculo(1L, vehiculoActualizado, usuarioEditor.getUsername()));

        verify(vehiculoRepository, times(1)).findById(1L);
        verify(vehiculoRepository, never()).save(any(Vehiculo.class));
        verify(vehiculoAuditRepository, never()).save(any(VehiculoAudit.class));
    }

    /**
     * Test para actualizar un vehículo con cambio de placa a una ya existente por un ADMIN.
     * Verifica que el servicio lance una IllegalArgumentException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al actualizar a placa ya existente por ADMIN")
    void whenActualizarVehiculo_toExistingPlacaByAdmin_thenThrowIllegalArgumentException() throws Exception {
        Vehiculo vehiculoActualizado = new Vehiculo(1L, "EXISTING-PLACA", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, conductor1); 
        Vehiculo existingVehiculoWithPlaca = new Vehiculo(99L, "EXISTING-PLACA", new BigDecimal("1000.00"), "Ford", "Fiesta", 2018, true, null);

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1)); 
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        when(vehiculoRepository.findByPlaca("EXISTING-PLACA")).thenReturn(Optional.of(existingVehiculoWithPlaca)); // Nueva placa ya existe

        assertThrows(IllegalArgumentException.class, () ->
                vehiculoService.actualizarVehiculo(1L, vehiculoActualizado, adminUser.getUsername()));

        verify(vehiculoRepository, times(1)).findById(1L);
        verify(vehiculoRepository, times(1)).findByPlaca("EXISTING-PLACA");
        verify(vehiculoRepository, never()).save(any(Vehiculo.class));
        verify(vehiculoAuditRepository, never()).save(any(VehiculoAudit.class));
    }


    /**
     * Test para eliminar un vehículo.
     * Verifica que el servicio lance una IllegalStateException debido a registros relacionados.
     */
    @Test
    @DisplayName("Debe lanzar IllegalStateException al intentar eliminar vehículo con registros relacionados")
    void whenEliminarVehiculo_thenThrowsIllegalStateExceptionDueToRelatedRecords() throws Exception {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1));
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        // Simular que la eliminación en el repositorio falla debido a una restricción de clave foránea
        doThrow(DataIntegrityViolationException.class).when(vehiculoRepository).delete(any(Vehiculo.class));

        assertThrows(IllegalStateException.class, () ->
                vehiculoService.eliminarVehiculo(1L, adminUser.getUsername()));

        verify(vehiculoRepository, times(1)).delete(any(Vehiculo.class));
        verify(vehiculoAuditRepository, never()).save(any(VehiculoAudit.class)); // La auditoría de eliminación no se debe registrar
    }

    /**
     * Test para el escenario donde se intenta eliminar un vehículo no encontrado.
     * Verifica que el servicio lance una EntityNotFoundException.
     */
    @Test
    @DisplayName("Debe lanzar excepción al intentar eliminar vehículo no encontrado")
    void whenEliminarVehiculoNotFound_thenThrowsEntityNotFoundException() {
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                vehiculoService.eliminarVehiculo(2L, adminUser.getUsername()));

        verify(vehiculoRepository, never()).delete(any(Vehiculo.class)); 
        verify(vehiculoAuditRepository, never()).save(any(VehiculoAudit.class));
    }

    /**
     * Test para cambiar el estado activo de un vehículo.
     * Verifica que el servicio actualice el estado y registre la auditoría.
     */
    @Test
    @DisplayName("Debe cambiar el estado activo de un vehículo y retornar 200 OK")
    void whenCambiarEstadoActivoVehiculo_thenReturnVehiculoConEstadoActualizado() throws Exception {
        vehiculo1.setActivo(true); 
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo1));
        when(vehiculoRepository.updateActivoStatus(1L, false)).thenReturn(1);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(adminUser);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Vehiculo resultado = vehiculoService.cambiarEstadoActivoVehiculo(1L, false, adminUser.getUsername());

        assertNotNull(resultado);
        assertFalse(resultado.getActivo());
        verify(vehiculoRepository, times(1)).updateActivoStatus(1L, false);
        verify(vehiculoAuditRepository, times(1)).save(any(VehiculoAudit.class));
    }

    /**
     * Test para obtener la lista de vehículos activos.
     * Verifica que el servicio retorne solo vehículos activos.
     */
    @Test
    @DisplayName("Debe obtener la lista de vehículos activos")
    void whenObtenerVehiculosActivos_thenReturnList() {
        Vehiculo activo1 = new Vehiculo(2L, "ACT-123", new BigDecimal("1000.00"), "MarcaA", "ModeloA", 2020, true, conductor1);
        Vehiculo activo2 = new Vehiculo(3L, "ACT-456", new BigDecimal("1500.00"), "MarcaB", "ModeloB", 2021, true, null);
        when(vehiculoRepository.findByActivoTrue()).thenReturn(Arrays.asList(activo1, activo2));

        List<Vehiculo> activos = vehiculoService.obtenerVehiculosActivos();

        assertNotNull(activos);
        assertEquals(2, activos.size());
        assertTrue(activos.stream().allMatch(Vehiculo::getActivo));
        verify(vehiculoRepository, times(1)).findByActivoTrue();
    }

    /**
     * Test para obtener la lista de vehículos inactivos.
     * Verifica que el servicio retorne solo vehículos inactivos.
     */
    @Test
    @DisplayName("Debe obtener la lista de vehículos inactivos")
    void whenObtenerVehiculosInactivos_thenReturnList() {
        Vehiculo inactivo1 = new Vehiculo(4L, "INA-789", new BigDecimal("800.00"), "MarcaC", "ModeloC", 2019, false, conductor1);
        when(vehiculoRepository.findByActivoFalse()).thenReturn(Arrays.asList(inactivo1));

        List<Vehiculo> inactivos = vehiculoService.obtenerVehiculosInactivos();

        assertNotNull(inactivos);
        assertEquals(1, inactivos.size());
        assertFalse(inactivos.get(0).getActivo());
        verify(vehiculoRepository, times(1)).findByActivoFalse();
    }

    /**
     * Test para obtener el historial de cambios de un vehículo por su ID.
     * Verifica que el servicio retorne la lista de auditorías.
     */
    @Test
    @DisplayName("Debe obtener el historial de cambios de un vehículo por su ID")
    void whenObtenerHistorialCambiosPorVehiculo_thenReturnList() {
        VehiculoAudit audit1 = new VehiculoAudit(vehiculo1, TipoOperacion.CREAR, adminUser, "detalles");
        when(vehiculoAuditRepository.findByVehiculoIdOrderByFechaCambioDesc(1L)).thenReturn(Collections.singletonList(audit1));

        List<VehiculoAudit> historial = vehiculoService.obtenerHistorialCambiosPorVehiculo(1L);

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(vehiculoAuditRepository, times(1)).findByVehiculoIdOrderByFechaCambioDesc(1L);
    }

    /**
     * Test para obtener el historial de cambios de un vehículo por placa.
     * Verifica que el servicio retorne la lista de auditorías.
     */
    @Test
    @DisplayName("Debe obtener el historial de cambios de un vehículo por placa")
    void whenObtenerHistorialCambiosPorPlaca_thenReturnList() throws JsonProcessingException {
        VehiculoAudit audit1 = new VehiculoAudit(vehiculo1, TipoOperacion.ACTUALIZAR, adminUser, "detalles");
        when(vehiculoAuditRepository.findByVehiculoPlacaOrderByFechaCambioDesc(anyString())).thenReturn(Collections.singletonList(audit1));
        
        List<VehiculoAudit> historial = vehiculoService.obtenerHistorialCambiosPorPlaca("XYZ-789");

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(vehiculoAuditRepository, times(1)).findByVehiculoPlacaOrderByFechaCambioDesc("XYZ-789");
    }

    /**
     * Test para el escenario donde no se encuentra historial de cambios para una placa dada.
     * Verifica que el servicio retorne una lista vacía, ya que el servicio no lanza excepción en este caso.
     */
    @Test
    @DisplayName("Debe retornar lista vacía al obtener historial por placa no existente")
    void whenObtenerHistorialCambiosPorPlacaNotFound_thenReturnEmptyList() {
        when(vehiculoAuditRepository.findByVehiculoPlacaOrderByFechaCambioDesc(anyString())).thenReturn(Collections.emptyList());

        List<VehiculoAudit> historial = vehiculoService.obtenerHistorialCambiosPorPlaca("NON-EXISTENT");
        
        assertNotNull(historial);
        assertTrue(historial.isEmpty());
        verify(vehiculoAuditRepository, times(1)).findByVehiculoPlacaOrderByFechaCambioDesc("NON-EXISTENT");
    }
}
