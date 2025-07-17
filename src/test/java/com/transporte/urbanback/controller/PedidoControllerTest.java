package com.transporte.urbanback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.security.SecurityUtils;
import com.transporte.urbanback.service.PedidoService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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
 * Clase de pruebas para PedidoController.
 * Utiliza @WebMvcTest para enfocar el testeo en la capa web.
 * Se usa @WithMockUser para simular la autenticación y autorización.
 * Se deshabilitan explícitamente los filtros de seguridad con @AutoConfigureMockMvc(addFilters = false).
 */
@WebMvcTest(controllers = PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Tests para PedidoController")
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private JwtUtil jwtUtil;

    private Pedido pedido1;
    private Cliente cliente1;
    private Conductor conductor1;
    private Vehiculo vehiculo1;
    private PedidoAudit pedidoAudit1;
    private String usernameEditor = "adminuser";

    /**
     * Configuración inicial para cada test.
     * Inicializa objetos de ejemplo.
     */
    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        cliente1 = new Cliente(1L, "Cliente Test", "111", "222", "Dir Cliente", true);
        conductor1 = new Conductor(1L, "Conductor Test", "333", LocalDate.of(1980, 1, 1), "444", true);
        vehiculo1 = new Vehiculo(1L, "VEH-123", new BigDecimal("1000.00"), "Marca", "Modelo", 2020, true, conductor1);

        pedido1 = new Pedido(1L, cliente1, "Origen", "Destino",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.PENDIENTE, vehiculo1, conductor1, new BigDecimal("50.00"), "Notas");

        pedidoAudit1 = new PedidoAudit();
        pedidoAudit1.setId(1L);
        pedidoAudit1.setPedido(pedido1);
        pedidoAudit1.setTipoOperacion(TipoOperacion.CREAR);
        pedidoAudit1.setFechaCambio(LocalDateTime.now());
        // El usuario editor en PedidoAudit se mapea en el DTO, no se necesita un mock directo aquí
    }

    /**
     * Test para la creación de un nuevo pedido.
     * Verifica que el endpoint POST /api/v1/pedidos retorne 201 Created.
     * Requiere rol ADMIN o CLIENTE.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe crear un nuevo pedido y retornar 201 Created")
    void whenCrearPedido_thenReturn201Created() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(pedidoService.crearPedido(any(Pedido.class), anyString())).thenReturn(pedido1);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pedido1.getId()))
                .andExpect(jsonPath("$.direccionOrigen").value(pedido1.getDireccionOrigen()));
    }

    /**
     * Test para obtener un pedido por su ID.
     * Verifica que el endpoint GET /api/v1/pedidos/{id} retorne 200 OK.
     * Accesible para ADMIN, CLIENTE propietario o CONDUCTOR asignado.
     */
    @Test
    @WithMockUser(roles = "CLIENTE", username = "testclient") // Simula un CLIENTE
    @DisplayName("Debe obtener un pedido por ID y retornar 200 OK (como CLIENTE propietario)")
    void whenObtenerPedidoPorIdAsCliente_thenReturn200Ok() throws Exception {
        // Asegurarse de que el principal.username en @PreAuthorize coincida con el mock
        when(pedidoService.esPedidoPertenecienteACliente(anyLong(), anyLong())).thenReturn(true);
        when(pedidoService.obtenerPedidoPorId(anyLong())).thenReturn(Optional.of(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido1.getId()))
                .andExpect(jsonPath("$.estado").value(pedido1.getEstado().name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe obtener un pedido por ID y retornar 200 OK (como ADMIN)")
    void whenObtenerPedidoPorIdAsAdmin_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidoPorId(anyLong())).thenReturn(Optional.of(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido1.getId()))
                .andExpect(jsonPath("$.estado").value(pedido1.getEstado().name()));
    }

    /**
     * Test para el escenario donde no se encuentra un pedido por ID.
     * Verifica que el endpoint GET /api/v1/pedidos/{id} retorne 404 Not Found.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe retornar 404 Not Found cuando el pedido no existe")
    void whenObtenerPedidoPorId_thenRetornar404NotFound() throws Exception {
        when(pedidoService.obtenerPedidoPorId(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/pedidos/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test para obtener todos los pedidos.
     * Verifica que el endpoint GET /api/v1/pedidos/todos retorne 200 OK con una lista.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener todos los pedidos y retornar 200 OK")
    void whenObtenerTodosLosPedidos_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerTodosLosPedidos()).thenReturn(Arrays.asList(pedido1,
                new Pedido(2L, cliente1, "Origen2", "Destino2", LocalDateTime.now(), null, null, null, null, EstadoPedido.ASIGNADO, vehiculo1, conductor1, new BigDecimal("60.00"), "Notas2")));

        mockMvc.perform(get("/api/v1/pedidos/todos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /todos/paginado debe retornar una página de pedidos")
    void pedidoController_whenGetPaginatedPedidos_thenReturnPageOfPedidos() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> expectedPage = new PageImpl<>(Collections.singletonList(pedido1), pageable, 1);
        when(pedidoService.obtenerTodosLosPedidosPaginados(any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/pedidos/todos/paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(pedido1.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(pedidoService, times(1)).obtenerTodosLosPedidosPaginados(any(Pageable.class));
    }

    /**
     * Test para actualizar un pedido existente.
     * Verifica que el endpoint PUT /api/v1/pedidos/{id} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe actualizar un pedido y retornar 200 OK")
    void whenActualizarPedido_thenReturn200Ok() throws Exception {
        Pedido pedidoActualizado = new Pedido(1L, cliente1, "Origen Actualizado", "Destino Actualizado",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.PENDIENTE, vehiculo1, conductor1, new BigDecimal("55.00"), "Notas Actualizadas");
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(pedidoService.actualizarPedido(anyLong(), any(Pedido.class), anyString())).thenReturn(pedidoActualizado);

        mockMvc.perform(put("/api/v1/pedidos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccionOrigen").value("Origen Actualizado"));
    }

    /**
     * Test para eliminar un pedido.
     * Verifica que el endpoint DELETE /api/v1/pedidos/{id} retorne 204 No Content.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe eliminar un pedido y retornar 204 No Content")
    void whenEliminarPedido_thenReturn204NoContent() throws Exception {
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        doNothing().when(pedidoService).eliminarPedido(anyLong(), anyString());

        mockMvc.perform(delete("/api/v1/pedidos/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    /**
     * Test para asignar conductor y vehículo a un pedido.
     * Verifica que el endpoint PUT /api/v1/pedidos/asignar/{pedidoId} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe asignar conductor y vehículo a un pedido y retornar 200 OK")
    void whenAsignarConductorYVehiculo_thenReturn200Ok() throws Exception {
        Pedido pedidoAsignado = new Pedido(1L, cliente1, "Origen", "Destino",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.ASIGNADO, vehiculo1, conductor1, new BigDecimal("50.00"), "Notas");
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(pedidoService.asignarConductorYVehiculo(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(pedidoAsignado);

        mockMvc.perform(put("/api/v1/pedidos/asignar/{pedidoId}", 1L)
                        .param("conductorId", "1")
                        .param("vehiculoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"));
    }

    /**
     * Test para cambiar el estado de un pedido.
     * Verifica que el endpoint PUT /api/v1/pedidos/estado/{pedidoId}/{nuevoEstado} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR asignado.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR", username = "testconductor") // Simula un CONDUCTOR
    @DisplayName("Debe cambiar el estado de un pedido y retornar 200 OK (como CONDUCTOR asignado)")
    void whenCambiarEstadoPedidoAsConductor_thenReturn200Ok() throws Exception {
        Pedido pedidoEnCamino = new Pedido(1L, cliente1, "Origen", "Destino",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.EN_CAMINO, vehiculo1, conductor1, new BigDecimal("50.00"), "Notas");
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn("testconductor"); // Mockear el username del conductor
        when(pedidoService.esPedidoAsignadoAConductor(anyLong(), anyLong())).thenReturn(true); // Usar anyString() para principal.username
        when(pedidoService.cambiarEstadoPedido(anyLong(), any(EstadoPedido.class), anyString())).thenReturn(pedidoEnCamino);

        mockMvc.perform(put("/api/v1/pedidos/estado/{pedidoId}/{nuevoEstado}", 1L, EstadoPedido.EN_CAMINO.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe cambiar el estado de un pedido y retornar 200 OK (como ADMIN)")
    void whenCambiarEstadoPedidoAsAdmin_thenReturn200Ok() throws Exception {
        Pedido pedidoEnCamino = new Pedido(1L, cliente1, "Origen", "Destino",
                LocalDateTime.now(), null, null, null, null,
                EstadoPedido.EN_CAMINO, vehiculo1, conductor1, new BigDecimal("50.00"), "Notas");
        when(securityUtils.obtenerNombreUsuarioAutenticado()).thenReturn(usernameEditor);
        when(pedidoService.cambiarEstadoPedido(anyLong(), any(EstadoPedido.class), anyString())).thenReturn(pedidoEnCamino);

        mockMvc.perform(put("/api/v1/pedidos/estado/{pedidoId}/{nuevoEstado}", 1L, EstadoPedido.EN_CAMINO.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));
    }

    /**
     * Test para obtener pedidos por cliente.
     * Verifica que el endpoint GET /api/v1/pedidos/por-cliente/{clienteId} retorne 200 OK.
     * Accesible para ADMIN o CLIENTE propietario.
     */
    @Test
    @WithMockUser(roles = "CLIENTE", username = "testclient") // Simula un CLIENTE
    @DisplayName("Debe obtener pedidos por cliente y retornar 200 OK (como CLIENTE propietario)")
    void whenObtenerPedidosPorClienteAsCliente_thenReturn200Ok() throws Exception {
        when(securityUtils.isClienteOwnedByCurrentUser(anyLong())).thenReturn(true);
        when(pedidoService.obtenerPedidosPorCliente(anyLong())).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-cliente/{clienteId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe obtener pedidos por cliente y retornar 200 OK (como ADMIN)")
    void whenObtenerPedidosPorClienteAsAdmin_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorCliente(anyLong())).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-cliente/{clienteId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    /**
     * Test para obtener pedidos por conductor.
     * Verifica que el endpoint GET /api/v1/pedidos/por-conductor/{conductorId} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR", username = "testconductor") // Simula un CONDUCTOR
    @DisplayName("Debe obtener pedidos por conductor y retornar 200 OK (como CONDUCTOR propietario)")
    void whenObtenerPedidosPorConductorAsConductor_thenReturn200Ok() throws Exception {
        when(securityUtils.isConductorIdLinkedToCurrentUser(anyLong())).thenReturn(true);
        when(pedidoService.obtenerPedidosPorConductor(anyLong())).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-conductor/{conductorId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe obtener pedidos por conductor y retornar 200 OK (como ADMIN)")
    void whenObtenerPedidosPorConductorAsAdmin_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorConductor(anyLong())).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-conductor/{conductorId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    /**
     * Test para obtener pedidos por estado.
     * Verifica que el endpoint GET /api/v1/pedidos/por-estado/{estado} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener pedidos por estado y retornar 200 OK")
    void whenObtenerPedidosPorEstado_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorEstado(any(EstadoPedido.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-estado/{estado}", EstadoPedido.PENDIENTE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value(EstadoPedido.PENDIENTE.name()));
    }

    /**
     * Test para obtener pedidos por rango de fechas de creación.
     * Verifica que el endpoint GET /api/v1/pedidos/por-fecha-creacion retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener pedidos por rango de fechas de creación y retornar 200 OK")
    void whenObtenerPedidosPorRangoFechasCreacion_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorRangoFechasCreacion(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-fecha-creacion")
                        .param("fechaInicio", "2023-01-01T00:00:00")
                        .param("fechaFin", "2023-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de un pedido por ID.
     * Verifica que el endpoint GET /api/v1/pedidos/auditoria/{pedidoId} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener historial de cambios por ID de pedido y retornar 200 OK")
    void whenObtenerHistorialCambiosPorPedido_thenReturn200Ok() throws Exception {
        // Para este test, necesitamos mockear el mapeo a DTO, ya que el controlador lo hace internamente.
        // O simplemente mockear el servicio para que devuelva una lista de PedidoAuditDTO directamente.
        // Optamos por mockear el servicio para que devuelva PedidoAudit y luego el controlador lo mapeará.
        when(pedidoService.obtenerHistorialCambiosPorPedido(anyLong())).thenReturn(Arrays.asList(pedidoAudit1));

        mockMvc.perform(get("/api/v1/pedidos/auditoria/{pedidoId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoAudit1.getId()));
    }

    /**
     * Test para obtener el historial de cambios de pedidos por usuario editor.
     * Verifica que el endpoint GET /api/v1/pedidos/auditoria/por-usuario/{usernameEditor} retorne 200 OK.
     * Requiere rol ADMIN.
     */
    @Test
    @WithMockUser(roles = "ADMIN") // Simula un usuario con rol ADMIN
    @DisplayName("Debe obtener historial de cambios por usuario editor y retornar 200 OK")
    void whenObtenerHistorialCambiosPorUsuarioEditor_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerHistorialCambiosPorUsuarioEditor(anyString())).thenReturn(Arrays.asList(pedidoAudit1));

        mockMvc.perform(get("/api/v1/pedidos/auditoria/por-usuario/{usernameEditor}", usernameEditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoAudit1.getId()));
    }

    /**
     * Test para obtener pedidos por conductor y estado.
     * Verifica que el endpoint GET /api/v1/pedidos/por-conductor/{conductorId}/estado/{estado} retorne 200 OK.
     * Accesible para ADMIN o CONDUCTOR propietario.
     */
    @Test
    @WithMockUser(roles = "CONDUCTOR", username = "testconductor") // Simula un CONDUCTOR
    @DisplayName("Debe obtener pedidos por conductor y estado y retornar 200 OK (como CONDUCTOR propietario)")
    void whenObtenerPedidosPorConductorYEstadoAsConductor_thenReturn200Ok() throws Exception {
        when(securityUtils.isConductorIdLinkedToCurrentUser(anyLong())).thenReturn(true);
        when(pedidoService.obtenerPedidosPorConductorYEstado(anyLong(), any(EstadoPedido.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-conductor/{conductorId}/estado/{estado}", 1L, EstadoPedido.PENDIENTE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe obtener pedidos por conductor y estado y retornar 200 OK (como ADMIN)")
    void whenObtenerPedidosPorConductorYEstadoAsAdmin_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorConductorYEstado(anyLong(), any(EstadoPedido.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-conductor/{conductorId}/estado/{estado}", 1L, EstadoPedido.PENDIENTE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    /**
     * Test para obtener pedidos por cliente y estado.
     * Verifica que el endpoint GET /api/v1/pedidos/por-cliente/{clienteId}/estado/{estado} retorne 200 OK.
     * Accesible para ADMIN o CLIENTE propietario.
     */
    @Test
    @WithMockUser(roles = "CLIENTE", username = "testclient") // Simula un CLIENTE
    @DisplayName("Debe obtener pedidos por cliente y estado y retornar 200 OK (como CLIENTE propietario)")
    void whenObtenerPedidosPorClienteYEstadoAsCliente_thenReturn200Ok() throws Exception {
        when(securityUtils.isClienteOwnedByCurrentUser(anyLong())).thenReturn(true);
        when(pedidoService.obtenerPedidosPorClienteYEstado(anyLong(), any(EstadoPedido.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-cliente/{clienteId}/estado/{estado}", 1L, EstadoPedido.PENDIENTE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simula un ADMIN
    @DisplayName("Debe obtener pedidos por cliente y estado y retornar 200 OK (como ADMIN)")
    void whenObtenerPedidosPorClienteYEstadoAsAdmin_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerPedidosPorClienteYEstado(anyLong(), any(EstadoPedido.class))).thenReturn(Arrays.asList(pedido1));

        mockMvc.perform(get("/api/v1/pedidos/por-cliente/{clienteId}/estado/{estado}", 1L, EstadoPedido.PENDIENTE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedido1.getId()));
    }

    /**
     * Test para obtener todos los estados de pedido.
     * Verifica que el endpoint GET /api/v1/pedidos/estados-de-pedido retorne 200 OK.
     * Público (no @PreAuthorize).
     */
    @Test
    @DisplayName("Debe obtener todos los estados de pedido y retornar 200 OK")
    void whenGetAllEstadosDePedido_thenReturn200Ok() throws Exception {
        when(pedidoService.obtenerTodosLosEstadosDePedido()).thenReturn(Arrays.asList(EstadoPedido.values()));

        mockMvc.perform(get("/api/v1/pedidos/estados-de-pedido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
