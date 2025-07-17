package com.transporte.urbanback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.dto.VehiculoAuditDTO;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.VehiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; 
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; 

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.transporte.urbanback.security.UserDetailsServiceImpl;
import com.transporte.urbanback.security.JwtUtil;


/**
 * Clase de pruebas para VehiculoController.
 * Utiliza @WebMvcTest para enfocar el testeo en la capa web.
 * Se usa @WithMockUser para simular la autenticación y autorización.
 * Se deshabilitan explícitamente los filtros de seguridad con @AutoConfigureMockMvc(addFilters = false).
 */
@WebMvcTest(controllers = VehiculoController.class) 
@AutoConfigureMockMvc(addFilters = false) 
@DisplayName("Tests para VehiculoController")
@ActiveProfiles("test")
class VehiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehiculoService vehiculoService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private JwtUtil jwtUtil;


    private Vehiculo vehiculo1;
    private Conductor conductor1;
    private VehiculoAudit vehiculoAudit1;
    private String usernameEditor = "adminuser";

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() {
        conductor1 = new Conductor(1L, "Conductor Test", "123456", LocalDate.of(1980, 1, 1), "1234567890", true);
        vehiculo1 = new Vehiculo(1L, "XYZ-789", new BigDecimal("1200.00"), "Toyota", "Corolla", 2020, true, conductor1);
        vehiculoAudit1 = new VehiculoAudit();
        vehiculoAudit1.setId(1L);
        vehiculoAudit1.setVehiculo(vehiculo1);
        vehiculoAudit1.setTipoOperacion(TipoOperacion.CREAR);
        vehiculoAudit1.setFechaCambio(LocalDateTime.now());
    }

    /**
     * Test para la creación de un nuevo vehículo.
     * Verifica que el endpoint POST /api/v1/vehiculos retorne 201 Created.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe crear un nuevo vehículo y retornar 201 Created")
    void whenCrearVehiculo_thenReturn201Created() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(vehiculoService.crearVehiculo(any(Vehiculo.class), anyString())).thenReturn(vehiculo1);

        mockMvc.perform(post("/api/v1/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehiculo1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vehiculo1.getId()))
                .andExpect(jsonPath("$.placa").value(vehiculo1.getPlaca()));
    }

    /**
     * Test para obtener un vehículo por su ID.
     * Verifica que el endpoint GET /api/v1/vehiculos/{id} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener vehículo por ID y retornar 200 OK")
    void whenObtenerVehiculoPorId_thenReturn200Ok() throws Exception {
        when(securityUtils.isVehiculoIdAssignedToCurrentUser(anyLong())).thenReturn(true);
        when(vehiculoService.obtenerVehiculoPorId(anyLong())).thenReturn(Optional.of(vehiculo1));

        mockMvc.perform(get("/api/v1/vehiculos/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehiculo1.getId()))
                .andExpect(jsonPath("$.placa").value(vehiculo1.getPlaca()));
    }

    /**
     * Test para el escenario donde no se encuentra un vehículo por ID.
     * Verifica que el endpoint GET /api/v1/vehiculos/{id} retorne 404 Not Found.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe retornar 404 Not Found cuando el vehículo no existe")
    void whenObtenerVehiculoPorId_thenRetornar404NotFound() throws Exception {
        when(securityUtils.isVehiculoIdAssignedToCurrentUser(anyLong())).thenReturn(true); // Simula que el PreAuthorize pasa
        when(vehiculoService.obtenerVehiculoPorId(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/vehiculos/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test para obtener todos los vehículos.
     * Verifica que el endpoint GET /api/v1/vehiculos retorne 200 OK con una lista.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener todos los vehículos y retornar 200 OK")
    void whenObtenerTodosLosVehiculos_thenReturn200Ok() throws Exception {
        when(vehiculoService.obtenerTodosLosVehiculos()).thenReturn(Arrays.asList(vehiculo1,
                new Vehiculo(2L, "ABC-456", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, null)));

        mockMvc.perform(get("/api/v1/vehiculos/todos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /todos/paginado debe retornar una página de vehículos")
    void vehiculoController_whenGetPaginatedVehiculos_thenReturnPageOfVehiculos() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehiculo> expectedPage = new PageImpl<>(Collections.singletonList(vehiculo1), pageable, 1);
        when(vehiculoService.obtenerTodosLosVehiculosPaginados(any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/vehiculos/todos/paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(vehiculo1.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(vehiculoService, times(1)).obtenerTodosLosVehiculosPaginados(any(Pageable.class));
    }

    /**
     * Test para actualizar un vehículo existente.
     * Verifica que el endpoint PUT /api/v1/vehiculos/{id} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe actualizar un vehículo y retornar 200 OK")
    void whenActualizarVehiculo_thenReturn200Ok() throws Exception {
        Vehiculo vehiculoActualizado = new Vehiculo(1L, "XYZ-789", new BigDecimal("1500.00"), "Honda", "CRV", 2022, true, conductor1);
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(vehiculoService.actualizarVehiculo(anyLong(), any(Vehiculo.class), anyString())).thenReturn(vehiculoActualizado);

        mockMvc.perform(put("/api/v1/vehiculos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehiculoActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Honda"));
    }

    /**
     * Test para eliminar un vehículo.
     * Verifica que el endpoint DELETE /api/v1/vehiculos/{id} retorne 204 No Content.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe eliminar un vehículo y retornar 204 No Content")
    void whenEliminarVehiculo_thenReturn204NoContent() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        doNothing().when(vehiculoService).eliminarVehiculo(anyLong(), anyString());

        mockMvc.perform(delete("/api/v1/vehiculos/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    /**
     * Test para cambiar el estado activo de un vehículo.
     * Verifica que el endpoint PATCH /api/v1/vehiculos/{id}/estado retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe cambiar el estado activo de un vehículo y retornar 200 OK")
    void whenCambiarEstadoActivoVehiculo_thenReturn200Ok() throws Exception {
        vehiculo1.setActivo(false);
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(vehiculoService.cambiarEstadoActivoVehiculo(anyLong(), anyBoolean(), anyString())).thenReturn(vehiculo1);

        mockMvc.perform(patch("/api/v1/vehiculos/estado/{id}", 1L)
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    /**
     * Test para obtener vehículos activos.
     * Verifica que el endpoint GET /api/v1/vehiculos/activos retorne 200 OK.
     * Accesible para ADMIN, CLIENTE o CONDUCTOR.
     */
    @Test
    @WithMockUser(roles = "CLIENTE") // Simula un usuario con rol CLIENTE (o ADMIN/CONDUCTOR)
    @DisplayName("Debe obtener vehículos activos y retornar 200 OK")
    void whenObtenerVehiculosActivos_thenReturn200Ok() throws Exception {
        Vehiculo activo1 = new Vehiculo(1L, "ACT-111", new BigDecimal("1000.00"), "Activa", "ModeloA", 2020, true, null);
        when(vehiculoService.obtenerVehiculosActivos()).thenReturn(Arrays.asList(activo1));

        mockMvc.perform(get("/api/v1/vehiculos/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("ACT-111"));
    }

    /**
     * Test para obtener vehículos inactivos.
     * Verifica que el endpoint GET /api/v1/vehiculos/inactivos retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener vehículos inactivos y retornar 200 OK")
    void whenObtenerVehiculosInactivos_thenReturn200Ok() throws Exception {
        Vehiculo inactivo1 = new Vehiculo(2L, "INA-222", new BigDecimal("800.00"), "Inactiva", "ModeloB", 2019, false, null);
        when(vehiculoService.obtenerVehiculosInactivos()).thenReturn(Arrays.asList(inactivo1));

        mockMvc.perform(get("/api/v1/vehiculos/inactivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("INA-222"));
    }

    /**
     * Test para obtener el historial de cambios de un vehículo por ID.
     * Verifica que el endpoint GET /api/v1/vehiculos/auditoria/{vehiculoId} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener historial de cambios por ID de vehículo y retornar 200 OK")
    void whenObtenerHistorialCambiosPorVehiculo_thenReturn200Ok() throws Exception {
        when(securityUtils.isVehiculoIdAssignedToCurrentUser(anyLong())).thenReturn(true);
        // Crear un VehiculoAuditDTO para mockear la respuesta del controlador
        VehiculoAuditDTO vehiculoAuditDTO = new VehiculoAuditDTO();
        vehiculoAuditDTO.setId(vehiculoAudit1.getId());
        vehiculoAuditDTO.setVehiculoId(vehiculo1.getId());
        vehiculoAuditDTO.setDetallesCambio("detalles");
        vehiculoAuditDTO.setTipoOperacion(TipoOperacion.CREAR.name());
        vehiculoAuditDTO.setFechaCambio(LocalDateTime.now());
        vehiculoAuditDTO.setUsuarioEditor(usernameEditor);

        when(vehiculoService.obtenerHistorialCambiosPorVehiculo(anyLong())).thenReturn(Arrays.asList(vehiculoAudit1));
        when(vehiculoService.obtenerVehiculoPorId(anyLong())).thenReturn(Optional.of(vehiculo1)); // Para evitar 404 si el historial está vacío

        mockMvc.perform(get("/api/v1/vehiculos/auditoria/{vehiculoId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehiculoAudit1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de un vehículo por placa.
     * Verifica que el endpoint GET /api/v1/vehiculos/auditoria/placa/{placa} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener historial de cambios por placa de vehículo y retornar 200 OK")
    void whenObtenerHistorialCambiosPorPlaca_thenReturn200Ok() throws Exception {
        when(securityUtils.isVehiculoPlacaAssignedToCurrentUser(anyString())).thenReturn(true);
        // Crear un VehiculoAuditDTO para mockear la respuesta del controlador
        VehiculoAuditDTO vehiculoAuditDTO = new VehiculoAuditDTO();
        vehiculoAuditDTO.setId(vehiculoAudit1.getId());
        vehiculoAuditDTO.setVehiculoId(vehiculo1.getId());
        vehiculoAuditDTO.setDetallesCambio("detalles");
        vehiculoAuditDTO.setTipoOperacion(TipoOperacion.CREAR.name());
        vehiculoAuditDTO.setFechaCambio(LocalDateTime.now());
        vehiculoAuditDTO.setUsuarioEditor(usernameEditor);

        when(vehiculoService.obtenerHistorialCambiosPorPlaca(anyString())).thenReturn(Arrays.asList(vehiculoAudit1));
        // Mockear obtenerVehiculoPorId para la verificación de existencia en el controlador
        when(vehiculoService.obtenerVehiculoPorId(anyLong())).thenReturn(Optional.of(vehiculo1)); // Asumiendo que se llama con un ID válido

        mockMvc.perform(get("/api/v1/vehiculos/auditoria/placa/{placa}", "XYZ-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehiculoAudit1.getId()));
    }

    /**
     * Test para el escenario donde no se encuentra un vehículo por placa para auditoría.
     * Verifica que el endpoint GET /api/v1/vehiculos/auditoria/placa/{placa} retorne 404 Not Found.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe retornar 404 Not Found cuando no se encuentra vehículo por placa para auditoría")
    void whenObtenerHistorialCambiosPorPlaca_thenRetornar404NotFound() throws Exception {
        when(securityUtils.isVehiculoPlacaAssignedToCurrentUser(anyString())).thenReturn(true); // Simula que el PreAuthorize pasa
        when(vehiculoService.obtenerHistorialCambiosPorPlaca(anyString())).thenReturn(Collections.emptyList()); // No historial
        // Mockear obtenerVehiculoPorId para la verificación de existencia en el controlador
        when(vehiculoService.obtenerVehiculoPorId(anyLong())).thenReturn(Optional.empty()); // No se encuentra el vehículo por ID

        mockMvc.perform(get("/api/v1/vehiculos/auditoria/placa/{placa}", "NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }
}
