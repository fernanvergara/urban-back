package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.ClienteAudit;
import com.transporte.urbanback.security.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para ClienteAuditRepository")
class ClienteAuditRepositoryTest {

    @Autowired
    private ClienteAuditRepository clienteAuditRepository;
    @Autowired
    private ClienteRepository clienteRepository; 
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Cliente cliente1;
    private Cliente cliente2;
    private Usuario adminUser;
    private Usuario regularUser;

    @BeforeEach
    void setUp() {
        clienteAuditRepository.deleteAll(); // Limpia la tabla de auditoría
        clienteRepository.deleteAll();      // Limpia la tabla de clientes
        usuarioRepository.deleteAll();      // Limpia la tabla de usuarios

        // 1. Crear usuarios de prueba (editor)
        adminUser = new Usuario(null, "admin_test", "password", Rol.ADMIN, null, null, true);
        regularUser = new Usuario(null, "user_test", "password", Rol.CLIENTE, null, null, true);
        adminUser = usuarioRepository.save(adminUser);
        regularUser = usuarioRepository.save(regularUser);

        // 2. Crear clientes de prueba (auditados)
        cliente1 = new Cliente(null, "Juan Pérez", "1020304050", "+573001111111", "Dir Cliente 1", true);
        cliente2 = new Cliente(null, "Maria García", "1020304051", "+573002222222", "Dir Cliente 2", true);
        cliente1 = clienteRepository.save(cliente1);
        cliente2 = clienteRepository.save(cliente2);

        // 3. Crear registros de auditoría
        ClienteAudit audit1 = new ClienteAudit(cliente1, TipoOperacion.CREAR, adminUser, "Cliente creado");
        // Asegúrate de que la fechaCambio se establece automáticamente o la seteas para ordenación
        audit1.setFechaCambio(LocalDateTime.now().minusDays(2)); // Más antiguo

        ClienteAudit audit2 = new ClienteAudit(cliente1, TipoOperacion.ACTUALIZAR, adminUser, "Cliente actualizado: telefono");
        audit2.setFechaCambio(LocalDateTime.now().minusDays(1)); // Intermedio

        ClienteAudit audit3 = new ClienteAudit(cliente2, TipoOperacion.CREAR, regularUser, "Cliente 2 creado");
        audit3.setFechaCambio(LocalDateTime.now()); // Más reciente

        clienteAuditRepository.save(audit1);
        clienteAuditRepository.save(audit2);
        clienteAuditRepository.save(audit3);
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un cliente específico ordenado por fecha de cambio descendente")
    void testFindByClienteIdOrderByFechaCambioDesc() {
        List<ClienteAudit> audits = clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(cliente1.getId());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(2); // Dos auditorías para cliente1
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // Más reciente
        assertThat(audits.get(1).getTipoOperacion()).isEqualTo(TipoOperacion.CREAR);    // Más antiguo
        assertThat(audits.get(0).getCliente().getId()).isEqualTo(cliente1.getId());
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un cliente por su identificación")
    void testFindByClienteIdentificacionOrderByFechaCambioDesc() {
        List<ClienteAudit> audits = clienteAuditRepository.findByClienteIdentificacionOrderByFechaCambioDesc(cliente1.getIdentificacion());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(2);
        assertThat(audits.get(0).getCliente().getIdentificacion()).isEqualTo(cliente1.getIdentificacion());
        assertThat(audits.get(1).getCliente().getIdentificacion()).isEqualTo(cliente1.getIdentificacion());
    }

    @Test
    @DisplayName("Obtener historial por el usuario que realizó el cambio")
    void testFindByUsuarioEditorUsernameOrderByFechaCambioDesc() {
        List<ClienteAudit> audits = clienteAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc(adminUser.getUsername());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(2); // Dos auditorías hechas por adminUser
        assertThat(audits.get(0).getUsuarioEditor().getUsername()).isEqualTo(adminUser.getUsername());
        assertThat(audits.get(1).getUsuarioEditor().getUsername()).isEqualTo(adminUser.getUsername());

        // Verificar que estén ordenadas correctamente (la más reciente de adminUser primero)
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR);
        assertThat(audits.get(1).getTipoOperacion()).isEqualTo(TipoOperacion.CREAR);
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de cliente inexistente")
    void testFindByClienteIdNotFound() {
        List<ClienteAudit> audits = clienteAuditRepository.findByClienteIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para una identificación de cliente inexistente")
    void testFindByClienteIdentificacionNotFound() {
        List<ClienteAudit> audits = clienteAuditRepository.findByClienteIdentificacionOrderByFechaCambioDesc("9999999999");
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un nombre de usuario editor inexistente")
    void testFindByUsuarioEditorUsernameNotFound() {
        List<ClienteAudit> audits = clienteAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc("non_existent_user");
        assertThat(audits).isEmpty();
    }
}