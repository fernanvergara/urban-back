package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.PedidoAudit;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.security.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para PedidoAuditRepository")
class PedidoAuditRepositoryTest {

    @Autowired
    private PedidoAuditRepository pedidoAuditRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ConductorRepository conductorRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Cliente cliente;
    private Conductor conductor;
    private Vehiculo vehiculo;
    private Pedido pedido1;
    private Pedido pedido2;
    private Usuario adminUser;
    private Usuario operadorUser;

    @BeforeEach
    void setUp() {
        pedidoAuditRepository.deleteAll();
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        conductorRepository.deleteAll();
        vehiculoRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Crear entidades relacionadas
        cliente = new Cliente(null, "Cliente Audit", "1112223334", "+573001001001", "Dir Cliente Audit", true);
        cliente = clienteRepository.save(cliente);

        conductor = new Conductor(null, "Conductor Audit", "4445556667", LocalDate.of(1990, 7, 1), "+573002002002", true);
        conductor = conductorRepository.save(conductor);

        vehiculo = new Vehiculo(null, "AUD-001", new BigDecimal("1500.00"), "MarcaAudit", "ModeloAudit", 2022, true, conductor);
        vehiculo = vehiculoRepository.save(vehiculo);

        adminUser = new Usuario(null, "admin.audit.ped", "password", com.transporte.urbanback.enums.Rol.ADMIN, null, null, true);
        operadorUser = new Usuario(null, "operador.audit.ped", "password", com.transporte.urbanback.enums.Rol.CLIENTE, null, null, true); // Asumiendo que CLENTE también puede ser editor
        adminUser = usuarioRepository.save(adminUser);
        operadorUser = usuarioRepository.save(operadorUser);

        // Crear pedidos
        pedido1 = new Pedido(
                null, cliente, "Origen A", "Destino B", LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1).plusHours(1), null,
                LocalDateTime.now().plusHours(1), null, EstadoPedido.PENDIENTE, vehiculo, conductor, new BigDecimal("50.00"), ""
        );
        pedido1 = pedidoRepository.save(pedido1);

        pedido2 = new Pedido(
                null, cliente, "Origen C", "Destino D", LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2).plusHours(1), null,
                LocalDateTime.now().plusHours(2), null, EstadoPedido.ASIGNADO, vehiculo, conductor, new BigDecimal("30.00"), ""
        );
        pedido2 = pedidoRepository.save(pedido2);

        // Crear registros de auditoría
        // Pedido 1
        PedidoAudit audit1_1 = new PedidoAudit(pedido1, TipoOperacion.CREAR, adminUser, "{\"estado\":\"PENDIENTE\"}");
        audit1_1.setFechaCambio(LocalDateTime.now().minusHours(4));

        PedidoAudit audit1_2 = new PedidoAudit(pedido1, TipoOperacion.ACTUALIZAR, operadorUser, "{\"estado\":\"ASIGNADO\"}");
        audit1_2.setFechaCambio(LocalDateTime.now().minusHours(2));

        PedidoAudit audit1_3 = new PedidoAudit(pedido1, TipoOperacion.ACTUALIZAR, adminUser, "{\"estado\":\"EN_CAMINO\"}");
        audit1_3.setFechaCambio(LocalDateTime.now().minusHours(1));

        // Pedido 2
        PedidoAudit audit2_1 = new PedidoAudit(pedido2, TipoOperacion.CREAR, adminUser, "{\"estado\":\"PENDIENTE\"}");
        audit2_1.setFechaCambio(LocalDateTime.now().minusHours(5));

        PedidoAudit audit2_2 = new PedidoAudit(pedido2, TipoOperacion.ACTUALIZAR, adminUser, "{\"estado\":\"ASIGNADO\"}");
        audit2_2.setFechaCambio(LocalDateTime.now().minusHours(3));


        pedidoAuditRepository.save(audit1_1);
        pedidoAuditRepository.save(audit1_2);
        pedidoAuditRepository.save(audit1_3);
        pedidoAuditRepository.save(audit2_1);
        pedidoAuditRepository.save(audit2_2);
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un pedido específico por ID")
    void testFindByPedidoIdOrderByFechaCambioDesc() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByPedidoIdOrderByFechaCambioDesc(pedido1.getId());
        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getPedido().getId()).isEqualTo(pedido1.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // Ultimo cambio en pedido1
        assertThat(audits.get(0).getFechaCambio()).isAfter(audits.get(1).getFechaCambio());
    }

    @Test
    @DisplayName("Obtener historial de auditoría por ID de usuario editor")
    void testFindByUsuarioEditorIdOrderByFechaCambioDesc() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(adminUser.getId());
        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(4); // audit1_1, audit1_3, audit2_1, audit2_2 (4 de admin, 1 de operador)
        assertThat(audits.get(0).getUsuarioEditor().getId()).isEqualTo(adminUser.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // El más reciente de adminUser
    }

    @Test
    @DisplayName("Obtener historial de auditoría por nombre de usuario editor")
    void testFindByUsuarioEditorUsernameOrderByFechaCambioDesc() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc(operadorUser.getUsername());
        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getUsuarioEditor().getUsername()).isEqualTo(operadorUser.getUsername());
        assertThat(audits.get(0).getPedido().getId()).isEqualTo(pedido1.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR);
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de pedido inexistente")
    void testFindByPedidoIdNotFound() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByPedidoIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de usuario editor inexistente")
    void testFindByUsuarioEditorIdNotFound() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un username de editor inexistente")
    void testFindByUsuarioEditorUsernameNotFound() {
        List<PedidoAudit> audits = pedidoAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc("non_existent_editor");
        assertThat(audits).isEmpty();
    }
}