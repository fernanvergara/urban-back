package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.security.UsuarioAudit;
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
@DisplayName("Tests para UsuarioAuditRepository")
class UsuarioAuditRepositoryTest {

    @Autowired
    private UsuarioAuditRepository usuarioAuditRepository;
    @Autowired
    private UsuarioRepository usuarioRepository; // Necesario para crear Usuarios (auditados y editores)

    private Usuario adminUser; // Será el editor principal
    private Usuario userToAudit1; // Usuario que será auditado
    private Usuario userToAudit2; // Otro usuario que será auditado
    private Usuario secondaryEditorUser; // Otro usuario que realizará cambios

    @BeforeEach
    void setUp() {
        usuarioAuditRepository.deleteAll(); 
        usuarioRepository.deleteAll();    

        // 1. Crear usuarios de prueba (editores y auditados)
        adminUser = new Usuario(null, "admin.editor", "pass123", Rol.ADMIN, null, null, true);
        adminUser = usuarioRepository.save(adminUser);

        secondaryEditorUser = new Usuario(null, "sec.editor", "pass456", Rol.CONDUCTOR, null, null, true);
        secondaryEditorUser = usuarioRepository.save(secondaryEditorUser);

        userToAudit1 = new Usuario(null, "audit.user1", "pass789", Rol.CLIENTE, null, null, true);
        userToAudit1 = usuarioRepository.save(userToAudit1);

        userToAudit2 = new Usuario(null, "audit.user2", "pass101", Rol.CLIENTE, null, null, true);
        userToAudit2 = usuarioRepository.save(userToAudit2);


        // 2. Crear registros de auditoría
        // Auditorías para userToAudit1
        UsuarioAudit audit1_1 = new UsuarioAudit(userToAudit1, TipoOperacion.CREAR, adminUser, "{\"username\":\"audit.user1\"}");
        audit1_1.setFechaCambio(LocalDateTime.now().minusHours(5)); // Más antiguo

        UsuarioAudit audit1_2 = new UsuarioAudit(userToAudit1, TipoOperacion.ACTUALIZAR, secondaryEditorUser, "{\"email\":\"audit1_new@example.com\"}");
        audit1_2.setFechaCambio(LocalDateTime.now().minusHours(3)); // Intermedio

        UsuarioAudit audit1_3 = new UsuarioAudit(userToAudit1, TipoOperacion.ACTUALIZAR, adminUser, "{\"activo\":\"false\"}");
        audit1_3.setFechaCambio(LocalDateTime.now().minusHours(1)); // Más reciente para userToAudit1 por adminUser

        // Auditorías para userToAudit2
        UsuarioAudit audit2_1 = new UsuarioAudit(userToAudit2, TipoOperacion.CREAR, adminUser, "{\"username\":\"audit.user2\"}");
        audit2_1.setFechaCambio(LocalDateTime.now().minusHours(4)); // Intermedio

        usuarioAuditRepository.save(audit1_1);
        usuarioAuditRepository.save(audit1_2);
        usuarioAuditRepository.save(audit1_3);
        usuarioAuditRepository.save(audit2_1);
    }

    @Test
    @DisplayName("Buscar cambios en un usuario por su ID (el usuario auditado), ordenado por fecha de cambio descendente")
    void testFindByUsuarioAuditadoIdOrderByFechaCambioDesc() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioAuditadoIdOrderByFechaCambioDesc(userToAudit1.getId());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3); // Tres auditorías para userToAudit1
        assertThat(audits.get(0).getUsuarioAuditado().getId()).isEqualTo(userToAudit1.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // Última operación en userToAudit1
        assertThat(audits.get(0).getFechaCambio()).isAfter(audits.get(1).getFechaCambio());
    }

    @Test
    @DisplayName("Buscar cambios realizados por un usuario por su ID (el usuario editor), ordenado por fecha de cambio descendente")
    void testFindByUsuarioEditorIdOrderByFechaCambioDesc() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(adminUser.getId());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3); // audit1_1, audit1_3, audit2_1 fueron hechos por adminUser
        assertThat(audits.get(0).getUsuarioEditor().getId()).isEqualTo(adminUser.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // El más reciente de adminUser
        assertThat(audits.get(0).getFechaCambio()).isAfter(audits.get(1).getFechaCambio());
    }

    @Test
    @DisplayName("Listar cambios en un usuario por su username (el usuario auditado), ordenado por fecha de cambio descendente")
    void testFindByUsuarioAuditadoUsernameOrderByFechaCambioDesc() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioAuditadoUsernameOrderByFechaCambioDesc(userToAudit1.getUsername());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getUsuarioAuditado().getUsername()).isEqualTo(userToAudit1.getUsername());
    }

    @Test
    @DisplayName("Listar cambios realizados por un usuario por su username (el usuario editor), ordenado por fecha de cambio descendente")
    void testFindByUsuarioEditorUsernameOrderByFechaCambioDesc() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc(secondaryEditorUser.getUsername());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(1); // Solo audit1_2 fue hecho por secondaryEditorUser
        assertThat(audits.get(0).getUsuarioEditor().getUsername()).isEqualTo(secondaryEditorUser.getUsername());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR);
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de usuario auditado inexistente")
    void testFindByUsuarioAuditadoIdNotFound() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioAuditadoIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de usuario editor inexistente")
    void testFindByUsuarioEditorIdNotFound() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioEditorIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un username de usuario auditado inexistente")
    void testFindByUsuarioAuditadoUsernameNotFound() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioAuditadoUsernameOrderByFechaCambioDesc("non_existent_audited");
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un username de usuario editor inexistente")
    void testFindByUsuarioEditorUsernameNotFound() {
        List<UsuarioAudit> audits = usuarioAuditRepository.findByUsuarioEditorUsernameOrderByFechaCambioDesc("non_existent_editor_user");
        assertThat(audits).isEmpty();
    }
}