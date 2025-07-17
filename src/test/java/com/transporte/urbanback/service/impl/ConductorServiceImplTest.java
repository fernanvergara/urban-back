package com.transporte.urbanback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.ConductorAuditRepository;
import com.transporte.urbanback.repository.ConductorRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Clase de pruebas unitarias para ConductorServiceImpl.
 * Utiliza Mockito para simular las dependencias (repositorios y ObjectMapper),
 * asegurando que solo se pruebe la lógica de negocio del servicio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para ConductorServiceImpl") 
class ConductorServiceImplTest {

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private ConductorAuditRepository conductorAuditRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConductorServiceImpl conductorService;

    private Conductor conductor1;
    private Usuario usuarioEditor;
    private Vehiculo vehiculoAsignado;

    private MockedStatic<Utilidades> mockedUtilidades;

    @BeforeEach
    void setUp() {
        conductor1 = new Conductor(1L, "Carlos Gomez", "1020304050", LocalDate.of(1980, 5, 10), "+573101234567", true);
        usuarioEditor = new Usuario(1L, "adminuser", "password123", Rol.ADMIN, null, null, true);
        vehiculoAsignado = new Vehiculo(1L, "ABC-123", new BigDecimal("500.00"), "Toyota", "Corolla", 2015, true, conductor1);
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
    @DisplayName("Debe crear un conductor exitosamente") 
    void whenCrearConductor_thenReturnConductorCreado() throws Exception {
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductor1);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Conductor nuevoConductor = conductorService.crearConductor(conductor1, usuarioEditor.getUsername());

        assertNotNull(nuevoConductor);
        assertEquals("Carlos Gomez", nuevoConductor.getNombreCompleto());
        verify(conductorRepository, times(1)).save(conductor1);
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear conductor con identificación existente")
    void whenCrearConductorWithExistingIdentificacion_thenThrowDataIntegrityViolationException() {
        when(conductorRepository.save(any(Conductor.class))).thenThrow(DataIntegrityViolationException.class);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);

        assertThrows(DataIntegrityViolationException.class, () ->
                conductorService.crearConductor(conductor1, usuarioEditor.getUsername()));

        verify(conductorRepository, times(1)).save(conductor1);
        verify(conductorAuditRepository, never()).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe obtener un conductor por su ID")
    void whenObtenerConductorPorId_thenReturnConductor() {
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor1));

        Conductor encontrado = conductorService.obtenerConductorPorId(1L).get();

        assertNotNull(encontrado);
        assertEquals("Carlos Gomez", encontrado.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener conductor por ID no existente")
    void whenObtenerConductorPorId_thenThrowEntityNotFoundException() {
        when(conductorRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                conductorService.obtenerConductorPorId(2L).orElseThrow(() -> new EntityNotFoundException("Conductor no encontrado con ID: " + 2L)));
    }

    @Test
    @DisplayName("ConductorServiceImpl: Debe obtener una página de conductores")
    void conductorServiceImpl_whenObtenerTodosLosConductoresPaginados_thenReturnPageOfConductores() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Conductor> expectedPage = new PageImpl<>(Collections.singletonList(conductor1), pageable, 1);
        when(conductorRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Conductor> result = conductorService.obtenerTodosLosConductoresPaginados(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(conductor1.getId(), result.getContent().get(0).getId());
        verify(conductorRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Debe actualizar un conductor exitosamente")
    void whenActualizarConductor_thenReturnConductorActualizado() throws Exception {
        Conductor conductorActualizado = new Conductor(1L, "Carlos A. Gomez", "1020304050", LocalDate.of(1980, 5, 10), "+573109876543", true);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor1));
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductorActualizado);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Conductor resultado = conductorService.actualizarConductor(1L, conductorActualizado, usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertEquals("Carlos A. Gomez", resultado.getNombreCompleto());
        assertEquals("+573109876543", resultado.getTelefono());
        verify(conductorRepository, times(1)).findById(1L);
        verify(conductorRepository, times(1)).save(any(Conductor.class));
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe eliminar un conductor sin vehículos asignados")
    void whenEliminarConductor_thenDeletesAndAudits() throws Exception {
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor1));
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(vehiculoRepository.countByConductor(any(Conductor.class))).thenReturn(0L);

        conductorService.eliminarConductor(1L, usuarioEditor.getUsername());

        verify(conductorRepository, times(1)).delete(any(Conductor.class));
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar conductor con vehículos asignados")
    void whenEliminarConductorWithAssignedVehicles_thenThrowsIllegalStateException() {
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor1));
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(vehiculoRepository.countByConductor(any(Conductor.class))).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
                conductorService.eliminarConductor(1L, usuarioEditor.getUsername()));

        verify(conductorRepository, never()).delete(any(Conductor.class));
        verify(conductorAuditRepository, never()).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe cambiar el estado activo de un conductor")
    void whenCambiarEstadoActivoConductor_thenReturnConductorConEstadoActualizado() throws Exception {
        conductor1.setActivo(true);
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor1));
        when(conductorRepository.updateActivoStatus(1L, false)).thenReturn(1);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Conductor resultado = conductorService.cambiarEstadoActivoConductor(1L, false, usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertFalse(resultado.getActivo());
        verify(conductorRepository, times(1)).updateActivoStatus(1L, false);
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe asignar un vehículo a un conductor")
    void whenAsignarVehiculo_thenReturnConductorActualizado() throws Exception {
        Vehiculo vehiculoSinConductor = new Vehiculo(2L, "DEF-456", new BigDecimal("1000.00"), "Ford", "Ranger", 2018, true, null);
        
        when(vehiculoRepository.findById(vehiculoAsignado.getId())).thenReturn(Optional.of(vehiculoSinConductor));
        when(conductorRepository.findById(conductor1.getId())).thenReturn(Optional.of(conductor1));
        when(vehiculoRepository.countByConductor(conductor1)).thenReturn(0L);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculoAsignado);
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Conductor resultado = conductorService.asignarVehiculo(conductor1.getId(), vehiculoAsignado.getId(), usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertEquals(conductor1.getId(), resultado.getId());
        verify(vehiculoRepository, times(1)).save(any(Vehiculo.class));
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al asignar vehículo si el conductor excede el límite")
    void whenAsignarVehiculoOverLimit_thenThrowsIllegalStateException() {
        Vehiculo vehiculoExistente = new Vehiculo(2L, "DEF-456", new BigDecimal("1000.00"), "Ford", "Ranger", 2018, true, null);

        when(conductorRepository.findById(anyLong())).thenReturn(Optional.of(conductor1));
        when(vehiculoRepository.findById(anyLong())).thenReturn(Optional.of(vehiculoExistente));
        when(vehiculoRepository.countByConductor(any(Conductor.class))).thenReturn(3L); 
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);

        assertThrows(IllegalStateException.class, () ->
                conductorService.asignarVehiculo(conductor1.getId(), vehiculoExistente.getId(), usuarioEditor.getUsername()));

        verify(vehiculoRepository, never()).save(any(Vehiculo.class));
        verify(conductorAuditRepository, never()).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe desasignar un vehículo de un conductor")
    void whenDesasignarVehiculo_thenReturnConductorActualizado() throws Exception {
        Vehiculo vehiculoConConductor = new Vehiculo(1L, "ABC-123", new BigDecimal("500.00"), "Toyota", "Corolla", 2015, true, conductor1);

        when(vehiculoRepository.findById(vehiculoConConductor.getId())).thenReturn(Optional.of(vehiculoConConductor));
        when(conductorRepository.findById(conductor1.getId())).thenReturn(Optional.of(conductor1));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo v = invocation.getArgument(0);
            v.setConductor(null);
            return v;
        });
        mockedUtilidades.when(() -> Utilidades.getUsuarioEditor(anyString())).thenReturn(usuarioEditor);
        when(objectMapper.writeValueAsString(any())).thenReturn("detalles_json");

        Conductor resultado = conductorService.desasignarVehiculo(conductor1.getId(), vehiculoConConductor.getId(), usuarioEditor.getUsername());

        assertNotNull(resultado);
        assertEquals(conductor1.getId(), resultado.getId());
        verify(vehiculoRepository, times(1)).save(any(Vehiculo.class));
        verify(conductorAuditRepository, times(1)).save(any(ConductorAudit.class));
    }

    @Test
    @DisplayName("Debe obtener la lista de conductores activos")
    void whenObtenerConductorActivos_thenReturnList() {
        Conductor activo1 = new Conductor(2L, "Conductor Activo 1", "ID1", LocalDate.of(1990,1,1), "Tel1", true);
        Conductor activo2 = new Conductor(3L, "Conductor Activo 2", "ID2", LocalDate.of(1991,1,1), "Tel2", true);
        when(conductorRepository.findByActivoTrue()).thenReturn(Arrays.asList(activo1, activo2));

        List<Conductor> activos = conductorService.obtenerConductorActivos();

        assertNotNull(activos);
        assertEquals(2, activos.size());
        assertTrue(activos.stream().allMatch(Conductor::getActivo));
        verify(conductorRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("Debe obtener la lista de conductores inactivos")
    void whenObtenerConductorInactivos_thenReturnList() {
        Conductor inactivo1 = new Conductor(4L, "Conductor Inactivo 1", "ID3", LocalDate.of(1985,1,1), "Tel3", false);
        when(conductorRepository.findByActivoFalse()).thenReturn(Arrays.asList(inactivo1));

        List<Conductor> inactivos = conductorService.obtenerConductorInactivos();

        assertNotNull(inactivos);
        assertEquals(1, inactivos.size());
        assertFalse(inactivos.get(0).getActivo());
        verify(conductorRepository, times(1)).findByActivoFalse();
    }

    @Test
    @DisplayName("Debe obtener el historial de cambios de un conductor por su ID")
    void whenObtenerHistorialCambiosPorConductor_thenReturnList() {
        ConductorAudit audit1 = new ConductorAudit(conductor1, TipoOperacion.CREAR, usuarioEditor, "detalles");
        when(conductorAuditRepository.findByConductorIdOrderByFechaCambioDesc(1L)).thenReturn(Collections.singletonList(audit1));

        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorConductor(1L);

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(conductorAuditRepository, times(1)).findByConductorIdOrderByFechaCambioDesc(1L);
    }

    @Test
    @DisplayName("Debe obtener el historial de cambios de un conductor por su identificación")
    void whenObtenerHistorialCambiosPorIdentificacion_thenReturnList() {
        ConductorAudit audit1 = new ConductorAudit(conductor1, TipoOperacion.ACTUALIZAR, usuarioEditor, "detalles");
        when(conductorRepository.findByIdentificacion(anyString())).thenReturn(Optional.of(conductor1) );
        when(conductorAuditRepository.findByConductorIdentificacionOrderByFechaCambioDesc(anyString())).thenReturn(Collections.singletonList(audit1));

        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorIdentificacion("1020304050");

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(conductorAuditRepository, times(1)).findByConductorIdentificacionOrderByFechaCambioDesc(conductor1.getIdentificacion());
    }

    @Test
    @DisplayName("Debe retornar lista vacía al obtener historial por identificación no existente")
    void whenObtenerHistorialCambiosPorIdentificacionNotFound_thenReturnEmptyList() {
        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorIdentificacion("nonexistent");

        assertNotNull(historial);
        assertTrue(historial.isEmpty()); 
        verify(conductorAuditRepository, never()).findByConductorIdOrderByFechaCambioDesc(anyLong()); 
    }

    @Test
    @DisplayName("Debe obtener el historial de cambios de conductores por nombre")
    void whenObtenerHistorialCambiosPorNombre_thenReturnList() {
        ConductorAudit audit1 = new ConductorAudit(conductor1, TipoOperacion.CREAR, usuarioEditor, "detalles");
        when(conductorAuditRepository.findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc(anyString()))
                .thenReturn(Collections.singletonList(audit1));

        List<ConductorAudit> historial = conductorService.obtenerHistorialCambiosPorNombre("Carlos");

        assertNotNull(historial);
        assertEquals(1, historial.size());
        verify(conductorAuditRepository, times(1)).findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc("Carlos");
    }
}
