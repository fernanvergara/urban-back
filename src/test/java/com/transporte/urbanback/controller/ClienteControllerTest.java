package com.transporte.urbanback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.urbanback.config.SecurityConfig;
import com.transporte.urbanback.dto.ClienteAuditDTO;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit;
import com.transporte.urbanback.security.JwtUtil;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.security.UserDetailsServiceImpl;
import com.transporte.urbanback.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

/**
 * Clase de pruebas para ClienteController.
 * Utiliza @WebMvcTest para enfocar el testeo en la capa web.
 * Se usa @WithMockUser para simular la autenticación y autorización.
 */
@WebMvcTest(
    controllers = ClienteController.class,
    excludeAutoConfiguration = SecurityAutoConfiguration.class, 
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class 
    )
)
@DisplayName("Tests para ClienteController")
@ActiveProfiles("test")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private SecurityUtils securityUtils; // Mockear SecurityUtils

    // Añadir MockBeans para las dependencias de seguridad que no son parte del controlador
    // pero son necesarias para que el contexto de Spring Security se cargue en @WebMvcTest
    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private JwtUtil jwtUtil;


    private Cliente cliente1;
    private ClienteAudit clienteAudit1;
    private String usernameEditor = "adminuser";

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() {
        cliente1 = new Cliente(1L, "Juan Perez Garcia", "1020304050", "+573001234567", "Calle Falsa 123", true);
        clienteAudit1 = new ClienteAudit();
        clienteAudit1.setId(1L);
        clienteAudit1.setCliente(cliente1);
        clienteAudit1.setTipoOperacion(TipoOperacion.CREAR);
        clienteAudit1.setFechaCambio(LocalDateTime.now());
        // No se setea usuarioEditor directamente en ClienteAudit para evitar dependencia circular o mockear Usuario
        // En el DTO de auditoría, el username se setea directamente.
    }

    /**
     * Test para la creación de un nuevo cliente.
     * Verifica que el endpoint POST /api/v1/clientes retorne 201 Created.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") 
    @DisplayName("Debe crear un nuevo cliente y retornar 201 Created")
    void whenCrearCliente_thenReturn201Created() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(clienteService.crearCliente(any(Cliente.class), anyString())).thenReturn(cliente1);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cliente1.getId()))
                .andExpect(jsonPath("$.nombreCompleto").value(cliente1.getNombreCompleto()));
    }

    /**
     * Test para obtener un cliente por su ID.
     * Verifica que el endpoint GET /api/v1/clientes/{id} retorne 200 OK.
     * Accesible para ADMIN o CLIENTE que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CLIENTE") 
    @DisplayName("Debe obtener un cliente por ID y retornar 200 OK")
    void whenObtenerClientePorId_thenReturn200Ok() throws Exception {
        when(securityUtils.isClienteOwnedByCurrentUser(anyLong())).thenReturn(true); 
        when(clienteService.obtenerClientePorId(anyLong())).thenReturn(Optional.of(cliente1));

        mockMvc.perform(get("/api/v1/clientes/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente1.getId()))
                .andExpect(jsonPath("$.nombreCompleto").value(cliente1.getNombreCompleto()));
    }

    /**
     * Test para el escenario donde no se encuentra un cliente por ID.
     * Verifica que el endpoint GET /api/v1/clientes/{id} retorne 404 Not Found.
     * Accesible para ADMIN o CLIENTE que sea propietario.
     */
    @Test
    @WithMockUser(roles = "ADMIN") 
    @DisplayName("Debe retornar 404 Not Found cuando el cliente no existe")
    void whenObtenerClientePorId_thenRetornar404NotFound() throws Exception {
        // No es necesario mockear isClienteOwnedByCurrentUser si el rol es ADMIN,
        // ya que la regla @PreAuthorize("hasRole('ADMIN')") se cumple primero.
        when(clienteService.obtenerClientePorId(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clientes/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test para obtener todos los clientes.
     * Verifica que el endpoint GET /api/v1/clientes retorne 200 OK con una lista.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") 
    @DisplayName("Debe obtener todos los clientes y retornar 200 OK")
    void whenObtenerTodosLosClientes_thenReturn200Ok() throws Exception {
        when(clienteService.obtenerTodosLosClientes()).thenReturn(Arrays.asList(cliente1,
                new Cliente(2L, "Maria Lopez", "1020304051", "+57987654321", "Avenida Siempre Viva 742", false)));

        mockMvc.perform(get("/api/v1/clientes/todos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName(" GET /todos/paginado debe retornar una página de clientes")
    void clienteController_whenGetPaginatedClientes_thenReturnPageOfClientes() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> expectedPage = new PageImpl<>(Collections.singletonList(cliente1), pageable, 1);
        when(clienteService.obtenerTodosLosClientesPaginados(any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/clientes/todos/paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cliente1.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(clienteService, times(1)).obtenerTodosLosClientesPaginados(any(Pageable.class));
    }

    /**
     * Test para actualizar un cliente existente.
     * Verifica que el endpoint PUT /api/v1/clientes/{id} retorne 200 OK.
     * Accesible para ADMIN o CLIENTE que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CLIENTE") // Simula un usuario con rol CLIENTE
    @DisplayName("Debe actualizar un cliente y retornar 200 OK")
    void whenActualizarCliente_thenReturn200Ok() throws Exception {
        Cliente clienteActualizado = new Cliente(1L, "Juan Perez Actualizado", "1020304050", "+573001234567", "Nueva Calle 123", true);
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor); 
        when(securityUtils.isClienteOwnedByCurrentUser(anyLong())).thenReturn(true); 
        when(clienteService.actualizarCliente(anyLong(), any(Cliente.class), anyString())).thenReturn(clienteActualizado);

        mockMvc.perform(put("/api/v1/clientes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Perez Actualizado"));
    }

    /**
     * Test para eliminar un cliente.
     * Verifica que el endpoint DELETE /api/v1/clientes/{id} retorne 204 No Content.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe eliminar un cliente y retornar 204 No Content")
    void whenEliminarCliente_thenReturn204NoContent() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        doNothing().when(clienteService).eliminarCliente(anyLong(), anyString());

        mockMvc.perform(delete("/api/v1/clientes/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    /**
     * Test para cambiar el estado activo de un cliente.
     * Verifica que el endpoint PATCH /api/v1/clientes/{id}/estado retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe cambiar el estado activo de un cliente y retornar 200 OK")
    void whenCambiarEstadoActivoCliente_thenReturn200Ok() throws Exception {
        cliente1.setActivo(false); // Simula el cambio de estado
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(clienteService.cambiarEstadoActivoCliente(anyLong(), anyBoolean(), anyString())).thenReturn(cliente1);

        mockMvc.perform(patch("/api/v1/clientes/{id}/estado", 1L)
                        .param("nuevoEstado", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    /**
     * Test para obtener clientes activos.
     * Verifica que el endpoint GET /api/v1/clientes/activos retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") 
    @DisplayName("Debe obtener clientes activos y retornar 200 OK")
    void whenObtenerClientesActivos_thenReturn200Ok() throws Exception {
        Cliente activo1 = new Cliente(1L, "Activo Uno", "111", "111", "Dir1", true);
        when(clienteService.obtenerClientesActivos()).thenReturn(Arrays.asList(activo1));

        mockMvc.perform(get("/api/v1/clientes/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCompleto").value("Activo Uno"));
    }

    /**
     * Test para obtener clientes inactivos.
     * Verifica que el endpoint GET /api/v1/clientes/inactivos retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener clientes inactivos y retornar 200 OK")
    void whenObtenerClientesInactivos_thenReturn200Ok() throws Exception {
        Cliente inactivo1 = new Cliente(3L, "Inactivo Uno", "333", "333", "Dir3", false);
        when(clienteService.obtenerClientesInactivos()).thenReturn(Arrays.asList(inactivo1));

        mockMvc.perform(get("/api/v1/clientes/inactivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCompleto").value("Inactivo Uno"));
    }

    /**
     * Test para obtener el historial de cambios de un cliente por ID.
     * Verifica que el endpoint GET /api/v1/clientes/{id}/auditoria retorne 200 OK.
     * Accesible para ADMIN o CLIENTE que sea propietario.
     */
    @Test
    @WithMockUser(roles = "CLIENTE") // Simula un usuario con rol CLIENTE
    @DisplayName("Debe obtener historial de cambios por ID de cliente y retornar 200 OK")
    void whenObtenerHistorialCambiosPorCliente_thenReturn200Ok() throws Exception {
        when(securityUtils.isClienteOwnedByCurrentUser(anyLong())).thenReturn(true);
        // Crear un ClienteAuditDTO para mockear la respuesta del controlador
        ClienteAuditDTO clienteAuditDTO = new ClienteAuditDTO();
        clienteAuditDTO.setId(clienteAudit1.getId());
        clienteAuditDTO.setClienteId(cliente1.getId());
        clienteAuditDTO.setDetallesCambio("detalles");
        clienteAuditDTO.setTipoOperacion(TipoOperacion.CREAR.name());
        clienteAuditDTO.setFechaCambio(LocalDateTime.now());
        clienteAuditDTO.setUsuarioEditor(usernameEditor);

        when(clienteService.obtenerHistorialCambiosPorCliente(anyLong())).thenReturn(Arrays.asList(clienteAudit1));

        mockMvc.perform(get("/api/v1/clientes/{id}/auditoria", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clienteAudit1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de un cliente por identificación.
     * Verifica que el endpoint GET /api/v1/clientes/auditoria/identificacion/{identificacion} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener historial de cambios por identificación de cliente y retornar 200 OK")
    void whenObtenerHistorialCambiosPorIdentificacion_thenReturn200Ok() throws Exception {
        // Crear un ClienteAuditDTO para mockear la respuesta del controlador
        ClienteAuditDTO clienteAuditDTO = new ClienteAuditDTO();
        clienteAuditDTO.setId(clienteAudit1.getId());
        clienteAuditDTO.setClienteId(cliente1.getId());
        clienteAuditDTO.setDetallesCambio("detalles");
        clienteAuditDTO.setTipoOperacion(TipoOperacion.CREAR.name());
        clienteAuditDTO.setFechaCambio(LocalDateTime.now());
        clienteAuditDTO.setUsuarioEditor(usernameEditor);

        when(clienteService.obtenerHistorialCambiosPorIdentificacion(anyString())).thenReturn(Arrays.asList(clienteAudit1));

        mockMvc.perform(get("/api/v1/clientes/auditoria/identificacion/{identificacion}", "1020304050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clienteAudit1.getId()));
    }
}
