package com.transporte.urbanback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.ConductorService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

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
 * Clase de pruebas para ConductorController.
 * Utiliza @WebMvcTest para enfocar el testeo en la capa web.
 * Se usa @WithMockUser para simular la autenticación y autorización.
 * Se deshabilitan explícitamente los filtros de seguridad con @AutoConfigureMockMvc(addFilters = false).
 */
@WebMvcTest(controllers = ConductorController.class) 
@AutoConfigureMockMvc(addFilters = false) 
@DisplayName("Tests para ConductorController")
class ConductorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConductorService conductorService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private JwtUtil jwtUtil;


    private Conductor conductor1;
    private ConductorAudit conductorAudit1;
    private String usernameEditor = "adminuser";

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() {
        conductor1 = new Conductor(1L, "Carlos Gomez", "1020304050", LocalDate.of(1980, 5, 10), "+573101234567", true);
        conductorAudit1 = new ConductorAudit();
        conductorAudit1.setId(1L);
        conductorAudit1.setConductor(conductor1);
        conductorAudit1.setTipoOperacion(TipoOperacion.CREAR);
        conductorAudit1.setFechaCambio(LocalDateTime.now());
    }

    /**
     * Test para obtener todos los conductores.
     * Verifica que el endpoint GET /api/v1/conductores retorne 200 OK con una lista.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener todos los conductores y retornar 200 OK")
    void whenObtenerTodosLosConductores_thenReturn200Ok() throws Exception {
        when(conductorService.obtenerTodosLosConductores()).thenReturn(Arrays.asList(conductor1,
                new Conductor(2L, "Ana Torres", "1020304051", LocalDate.of(1990, 1, 1), "+573109876543", true)));

        mockMvc.perform(get("/api/v1/conductores/todos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /todos/paginado debe retornar una página de conductores")
    void conductorController_whenGetPaginatedConductores_thenReturnPageOfConductores() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Conductor> expectedPage = new PageImpl<>(Collections.singletonList(conductor1), pageable, 1);
        when(conductorService.obtenerTodosLosConductoresPaginados(any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/conductores/todos/paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(conductor1.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(conductorService, times(1)).obtenerTodosLosConductoresPaginados(any(Pageable.class));
    }

    /**
     * Test para obtener un conductor por su ID.
     * Verifica que el endpoint GET /api/v1/conductores/{id} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener conductor por ID y retornar 200 OK")
    void whenObtenerConductorPorId_thenReturn200Ok() throws Exception {
        when(securityUtils.isConductorIdLinkedToCurrentUser(anyLong())).thenReturn(true); // Simula que el conductor pertenece al usuario
        when(conductorService.obtenerConductorPorId(anyLong())).thenReturn(Optional.of(conductor1));

        mockMvc.perform(get("/api/v1/conductores/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conductor1.getId()))
                .andExpect(jsonPath("$.nombreCompleto").value(conductor1.getNombreCompleto()));
    }

    /**
     * Test para el escenario donde no se encuentra un conductor por ID.
     * Verifica que el endpoint GET /api/v1/conductores/{id} retorne 404 Not Found.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe retornar 404 Not Found cuando el conductor no existe")
    void whenObtenerConductorPorId_thenRetornar404NotFound() throws Exception {
        when(conductorService.obtenerConductorPorId(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/conductores/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test para la creación de un nuevo conductor.
     * Verifica que el endpoint POST /api/v1/conductores retorne 201 Created.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe crear un nuevo conductor y retornar 201 Created")
    void whenCrearConductor_thenReturn201Created() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(conductorService.crearConductor(any(Conductor.class), anyString())).thenReturn(conductor1);

        mockMvc.perform(post("/api/v1/conductores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conductor1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(conductor1.getId()))
                .andExpect(jsonPath("$.nombreCompleto").value(conductor1.getNombreCompleto()));
    }

    /**
     * Test para actualizar un conductor existente.
     * Verifica que el endpoint PUT /api/v1/conductores/{id} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe actualizar un conductor y retornar 200 OK")
    void whenActualizarConductor_thenReturn200Ok() throws Exception {
        Conductor conductorActualizado = new Conductor(1L, "Carlos A. Gomez", "1020304050", LocalDate.of(1980, 5, 10), "+573109876543", true);
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(conductorService.actualizarConductor(anyLong(), any(Conductor.class), anyString())).thenReturn(conductorActualizado);

        mockMvc.perform(put("/api/v1/conductores/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conductorActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos A. Gomez"));
    }

    /**
     * Test para eliminar un conductor.
     * Verifica que el endpoint DELETE /api/v1/conductores/{id} retorne 204 No Content.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe eliminar un conductor y retornar 204 No Content")
    void whenEliminarConductor_thenReturn204NoContent() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        doNothing().when(conductorService).eliminarConductor(anyLong(), anyString());

        mockMvc.perform(delete("/api/v1/conductores/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    /**
     * Test para cambiar el estado activo de un conductor.
     * Verifica que el endpoint PATCH /api/v1/conductores/{id}/estado retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe cambiar el estado activo de un conductor y retornar 200 OK")
    void whenCambiarEstadoActivoConductor_thenReturn200Ok() throws Exception {
        conductor1.setActivo(false);
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(conductorService.cambiarEstadoActivoConductor(anyLong(), anyBoolean(), anyString())).thenReturn(conductor1);

        mockMvc.perform(patch("/api/v1/conductores/{id}/estado", 1L)
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    /**
     * Test para obtener el historial de cambios de un conductor por ID.
     * Verifica que el endpoint GET /api/v1/conductores/auditoria/{conductorId} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener historial de cambios por ID de conductor y retornar 200 OK")
    void whenObtenerHistorialPorIdConductor_thenReturn200Ok() throws Exception {
        when(securityUtils.isConductorIdLinkedToCurrentUser(anyLong())).thenReturn(true);
        when(conductorService.obtenerHistorialCambiosPorConductor(anyLong())).thenReturn(Arrays.asList(conductorAudit1));
        when(conductorService.obtenerConductorPorId(anyLong())).thenReturn(Optional.of(conductor1)); // Para evitar 404 si el historial está vacío

        mockMvc.perform(get("/api/v1/conductores/auditoria/{conductorId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conductorAudit1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de un conductor por identificación.
     * Verifica que el endpoint GET /api/v1/conductores/auditoria/identificacion/{identificacion} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR") // Simula un usuario con rol CONDUCTOR
    @DisplayName("Debe obtener historial de cambios por identificación de conductor y retornar 200 OK")
    void whenObtenerHistorialPorIdentificacion_thenReturn200Ok() throws Exception {
        when(securityUtils.isConductorIdentificacionLinkedToCurrentUser(anyString())).thenReturn(true);
        when(conductorService.obtenerHistorialCambiosPorIdentificacion(anyString())).thenReturn(Arrays.asList(conductorAudit1));

        mockMvc.perform(get("/api/v1/conductores/auditoria/identificacion/{identificacion}", "1020304050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conductorAudit1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de conductores por nombre.
     * Verifica que el endpoint GET /api/v1/conductores/auditoria/nombre/{nombre} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener historial de cambios por nombre de conductor y retornar 200 OK")
    void whenObtenerHistorialPorNombre_thenReturn200Ok() throws Exception {
        when(conductorService.obtenerHistorialCambiosPorNombre(anyString())).thenReturn(Arrays.asList(conductorAudit1));

        mockMvc.perform(get("/api/v1/conductores/auditoria/nombre/{nombre}", "Carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conductorAudit1.getId()));
    }
}
