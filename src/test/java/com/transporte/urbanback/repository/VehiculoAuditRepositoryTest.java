package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.model.VehiculoAudit;
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
@DisplayName("Tests para VehiculoAuditRepository")
class VehiculoAuditRepositoryTest {

    @Autowired
    private VehiculoAuditRepository vehiculoAuditRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private ConductorRepository conductorRepository; // Necesario para crear Conductores
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Conductor conductor;
    private Vehiculo vehiculo1;
    private Vehiculo vehiculo2;
    private Usuario adminUser;
    private Usuario tallerUser;

    @BeforeEach
    void setUp() {
        vehiculoAuditRepository.deleteAll();
        vehiculoRepository.deleteAll();
        conductorRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Crear entidades relacionadas
        conductor = new Conductor(null, "Conductor Vehiculo Audit", "9876543210", LocalDate.of(1975, 3, 20), "+573009998877", true);
        conductor = conductorRepository.save(conductor);

        adminUser = new Usuario(null, "admin.veh.aud", "password", com.transporte.urbanback.enums.Rol.ADMIN, null, null, true); 
        tallerUser = new Usuario(null, "taller.veh.aud", "password", com.transporte.urbanback.enums.Rol.CONDUCTOR, null, null, true); // Asumiendo que CONDUCTOR puede ser editor
        adminUser = usuarioRepository.save(adminUser);
        tallerUser = usuarioRepository.save(tallerUser);

        // Crear vehículos
        vehiculo1 = new Vehiculo(null, "AUD-111", new BigDecimal("2000.00"), "Toyota", "Corolla", 2020, true, conductor);
        vehiculo1 = vehiculoRepository.save(vehiculo1);

        vehiculo2 = new Vehiculo(null, "AUD-222", new BigDecimal("3000.00"), "Ford", "Fiesta", 2018, true, conductor);
        vehiculo2 = vehiculoRepository.save(vehiculo2);

        // Crear registros de auditoría
        // Vehiculo 1
        VehiculoAudit audit1_1 = new VehiculoAudit(vehiculo1, TipoOperacion.CREAR, adminUser, "{\"marca\":\"Toyota\"}");
        audit1_1.setFechaCambio(LocalDateTime.now().minusHours(4));

        VehiculoAudit audit1_2 = new VehiculoAudit(vehiculo1, TipoOperacion.ACTUALIZAR, tallerUser, "{\"modelo\":\"Corolla GT\"}");
        audit1_2.setFechaCambio(LocalDateTime.now().minusHours(2));

        VehiculoAudit audit1_3 = new VehiculoAudit(vehiculo1, TipoOperacion.ACTUALIZAR, adminUser, "{\"activo\":\"false\"}");
        audit1_3.setFechaCambio(LocalDateTime.now().minusHours(1));

        // Vehiculo 2
        VehiculoAudit audit2_1 = new VehiculoAudit(vehiculo2, TipoOperacion.CREAR, adminUser, "{\"marca\":\"Ford\"}");
        audit2_1.setFechaCambio(LocalDateTime.now().minusHours(5));


        vehiculoAuditRepository.save(audit1_1);
        vehiculoAuditRepository.save(audit1_2);
        vehiculoAuditRepository.save(audit1_3);
        vehiculoAuditRepository.save(audit2_1);
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un vehículo específico por ID")
    void testFindByVehiculoIdOrderByFechaCambioDesc() {
        List<VehiculoAudit> audits = vehiculoAuditRepository.findByVehiculoIdOrderByFechaCambioDesc(vehiculo1.getId());
        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getVehiculo().getId()).isEqualTo(vehiculo1.getId());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR); // Último cambio en vehiculo1
        assertThat(audits.get(0).getFechaCambio()).isAfter(audits.get(1).getFechaCambio());
    }

    @Test
    @DisplayName("Obtener historial de auditoría para un vehículo por su placa")
    void testFindByVehiculoPlacaOrderByFechaCambioDesc() {
        List<VehiculoAudit> audits = vehiculoAuditRepository.findByVehiculoPlacaOrderByFechaCambioDesc(vehiculo1.getPlaca());
        assertThat(audits).isNotNull();
        assertThat(audits).hasSize(3);
        assertThat(audits.get(0).getVehiculo().getPlaca()).isEqualTo(vehiculo1.getPlaca());
        assertThat(audits.get(0).getTipoOperacion()).isEqualTo(TipoOperacion.ACTUALIZAR);
    }

    @Test
    @DisplayName("No debería encontrar auditorías para un ID de vehículo inexistente")
    void testFindByVehiculoIdNotFound() {
        List<VehiculoAudit> audits = vehiculoAuditRepository.findByVehiculoIdOrderByFechaCambioDesc(999L);
        assertThat(audits).isEmpty();
    }

    @Test
    @DisplayName("No debería encontrar auditorías para una placa de vehículo inexistente")
    void testFindByVehiculoPlacaNotFound() {
        List<VehiculoAudit> audits = vehiculoAuditRepository.findByVehiculoPlacaOrderByFechaCambioDesc("NON-EXT");
        assertThat(audits).isEmpty();
    }
}