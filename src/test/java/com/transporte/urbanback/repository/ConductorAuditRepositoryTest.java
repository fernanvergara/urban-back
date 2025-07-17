package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.ConductorAudit;
import com.transporte.urbanback.security.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para ConductorAuditRepository")
class ConductorAuditRepositoryTest {

    @Autowired
    private ConductorAuditRepository conductorAuditRepository;
    @Autowired
    private ConductorRepository conductorRepository; 
    @Autowired
    private UsuarioRepository usuarioRepository; 

    private Conductor conductor1;
    private Conductor conductor2;
    private Usuario adminUser;
    private Usuario supervisorUser;

    @BeforeEach
    void setUp() {
        conductorAuditRepository.deleteAll(); // Limpia la tabla de auditoría
        conductorRepository.deleteAll();      // Limpia la tabla de conductores
        usuarioRepository.deleteAll();        // Limpia la tabla de usuarios

        // 1. Crear usuarios de prueba (editor)
        adminUser = new Usuario(null, "admin_auditor", "password", com.transporte.urbanback.enums.Rol.ADMIN, null, null, true);
        supervisorUser = new Usuario(null, "supervisor_auditor", "password", com.transporte.urbanback.enums.Rol.CONDUCTOR, null, null, true);
        adminUser = usuarioRepository.save(adminUser);
        supervisorUser = usuarioRepository.save(supervisorUser);

        // 2. Crear conductores de prueba (auditados)
        conductor1 = new Conductor(null, "Roberto Gómez Bolaños", "1234567890", LocalDate.of(1929, 2, 21), "+573001002000", true);
        conductor2 = new Conductor(null, "Chespirito", "0987654321", LocalDate.of(1929, 2, 21), "+573003004000", false);
        conductor1 = conductorRepository.save(conductor1);
        conductor2 = conductorRepository.save(conductor2);

        // 3. Crear registros de auditoría
        ConductorAudit audit1 = new ConductorAudit(conductor1, TipoOperacion.CREAR, adminUser, "Conductor creado por admin");
        audit1.setFechaCambio(LocalDateTime.now().minusHours(5)); // Más antiguo

        ConductorAudit audit2 = new ConductorAudit(conductor1, TipoOperacion.ACTUALIZAR, supervisorUser, "Conductor actualizado por supervisor: telefono");
        audit2.setFechaCambio(LocalDateTime.now().minusHours(2)); // Intermedio

        ConductorAudit audit3 = new ConductorAudit(conductor2, TipoOperacion.CREAR, adminUser, "Conductor 2 creado por admin");
        audit3.setFechaCambio(LocalDateTime.now().minusHours(1)); // Intermedio

        ConductorAudit audit4 = new ConductorAudit(conductor1, TipoOperacion.ELIMINAR, adminUser, "Conductor eliminado (lógicamente) por admin");
        audit4.setFechaCambio(LocalDateTime.now()); // Más reciente

        conductorAuditRepository.save(audit1);
        conductorAuditRepository.save(audit2);
        conductorAuditRepository.save(audit3);
        conductorAuditRepository.save(audit4);
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un conductor específico por su ID, ordenado por fecha de cambio descendente")
    void testFindByConductorIdOrderByFechaCambioDesc() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorIdOrderByFechaCambioDesc(conductor1.getId());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3); // Tres auditorías para conductor1
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ELIMINAR); // Más reciente
        assertThat(audits.get(1).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // Intermedio
        assertThat(audits.get(2).getTipoOperacion()).isEqualTo(TipoOperacion.CREAR);    // Más antiguo
        assertThat(audits.get(0).getConductor().getId()).isEqualTo(conductor1.getId());
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un conductor por su identificación")
    void testFindByConductorIdentificacionOrderByFechaCambioDesc() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorIdentificacionOrderByFechaCambioDesc(conductor1.getIdentificacion());

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getConductor().getIdentificacion()).isEqualTo(conductor1.getIdentificacion());
    }

    @Test
    @DisplayName("Obtener historial para conductores cuyo nombre completo contiene la cadena dada")
    void testFindByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc("gómez");

        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3); // Audits for "Roberto Gómez Bolaños"
        assertThat(audits.get(0).getConductor().getNombreCompleto()).containsIgnoringCase("gómez");
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de conductor inexistente")
    void testFindByConductorIdNotFound() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para una identificación de conductor inexistente")
    void testFindByConductorIdentificacionNotFound() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorIdentificacionOrderByFechaCambioDesc("9999999999");
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un nombre de conductor inexistente")
    void testFindByConductorNombreCompletoContainingIgnoreCaseNotFound() {
        List<ConductorAudit> audits = conductorAuditRepository.findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc("NonExistentName");
        assertThat(audits).isEmpty();
    }
}