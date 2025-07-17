package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para VehiculoRepository")
class VehiculoRepositoryTest {

    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private ConductorRepository conductorRepository; 
    @Autowired
    private TestEntityManager entityManager;

    private Conductor conductor1;
    private Conductor conductor2;
    private Vehiculo vehiculo1;
    private Vehiculo vehiculo2;
    private Vehiculo vehiculo3;

    @BeforeEach
    void setUp() {
        vehiculoRepository.deleteAll();
        conductorRepository.deleteAll();

        conductor1 = new Conductor(null, "Mario Bros", "1234567891", LocalDate.of(1985, 9, 13), "+573001111111", true);
        conductor2 = new Conductor(null, "Luigi Bros", "1234567892", LocalDate.of(1987, 10, 1), "+573002222222", true);
        conductor1 = conductorRepository.save(conductor1);
        conductor2 = conductorRepository.save(conductor2);

        vehiculo1 = new Vehiculo(null, "ABC-123", new BigDecimal("1500.50"), "Toyota", "Hilux", 2018, true, conductor1);
        vehiculo2 = new Vehiculo(null, "DEF-456", new BigDecimal("2000.00"), "Ford", "Ranger", 2020, false, conductor1); 
        vehiculo3 = new Vehiculo(null, "GHI-789", new BigDecimal("1000.00"), "Nissan", "Frontier", 2019, true, conductor2);

        vehiculo1 = vehiculoRepository.save(vehiculo1);
        vehiculo2 = vehiculoRepository.save(vehiculo2);
        vehiculo3 = vehiculoRepository.save(vehiculo3);
    }

    @Test
    @DisplayName("Guardar Vehículo - Operación de Creación")
    void testSaveVehiculo() {
        Vehiculo nuevoVehiculo = new Vehiculo(null, "JKL-012", new BigDecimal("3000.00"), "Chevrolet", "Silverado", 2022, true, conductor1);
        Vehiculo vehiculoGuardado = vehiculoRepository.save(nuevoVehiculo);

        assertThat(vehiculoGuardado).isNotNull();
        assertThat(vehiculoGuardado.getId()).isNotNull();
        assertThat(vehiculoGuardado.getPlaca()).isEqualTo("JKL-012");
    }

    @Test
    @DisplayName("Buscar Vehículo por ID")
    void testFindById() {
        Optional<Vehiculo> foundVehiculo = vehiculoRepository.findById(vehiculo1.getId());
        assertThat(foundVehiculo).isPresent();
        assertThat(foundVehiculo.get().getPlaca()).isEqualTo("ABC-123");
    }

    @Test
    @DisplayName("Buscar todos los Vehículos")
    void testFindAllVehiculos() {
        List<Vehiculo> vehiculos = vehiculoRepository.findAll();
        assertThat(vehiculos).isNotNull();
        assertThat(vehiculos.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Actualizar Vehículo - Operación de Actualización")
    void testUpdateVehiculo() {
        Vehiculo vehiculoToUpdate = vehiculoRepository.findById(vehiculo1.getId()).get();
        vehiculoToUpdate.setCapacidadKg(new BigDecimal("1600.00"));
        vehiculoToUpdate.setModelo("Hilux Nueva");

        Vehiculo updatedVehiculo = vehiculoRepository.save(vehiculoToUpdate);

        assertThat(updatedVehiculo.getCapacidadKg()).isEqualTo(new BigDecimal("1600.00"));
        assertThat(updatedVehiculo.getModelo()).isEqualTo("Hilux Nueva");
    }

    @Test
    @DisplayName("Eliminar Vehículo por ID - Operación de Eliminación")
    void testDeleteById() {
        vehiculoRepository.deleteById(vehiculo1.getId());
        Optional<Vehiculo> deletedVehiculo = vehiculoRepository.findById(vehiculo1.getId());
        assertThat(deletedVehiculo).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Vehículo por Placa")
    void testFindByPlaca() {
        Optional<Vehiculo> foundVehiculo = vehiculoRepository.findByPlaca("ABC-123");
        assertThat(foundVehiculo).isPresent();
        assertThat(foundVehiculo.get().getMarca()).isEqualTo("Toyota");

        Optional<Vehiculo> notFoundVehiculo = vehiculoRepository.findByPlaca("XYZ-999");
        assertThat(notFoundVehiculo).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Vehículos Activos")
    void testFindByActivoTrue() {
        List<Vehiculo> activos = vehiculoRepository.findByActivoTrue();
        assertThat(activos).isNotNull();
        assertThat(activos.size()).isEqualTo(2); // vehiculo1 y vehiculo3
        assertThat(activos.stream().anyMatch(v -> v.getPlaca().equals("ABC-123"))).isTrue();
        assertThat(activos.stream().anyMatch(v -> v.getPlaca().equals("GHI-789"))).isTrue();
    }

    @Test
    @DisplayName("Buscar Vehículos Inactivos")
    void testFindByActivoFalse() {
        List<Vehiculo> inactivos = vehiculoRepository.findByActivoFalse();
        assertThat(inactivos).isNotNull();
        assertThat(inactivos.size()).isEqualTo(1);
        assertThat(inactivos.get(0).getPlaca()).isEqualTo("DEF-456");
    }

    @Test
    @Transactional
    @DisplayName("Actualizar estado activo de Vehículo directamente con @Query")
    void testUpdateActivoStatus() {
        // Verificar estado inicial
        assertThat(vehiculoRepository.findById(vehiculo1.getId()).get().getActivo()).isTrue();

        // Actualizar estado a false
        int updatedRows = vehiculoRepository.updateActivoStatus(vehiculo1.getId(), false);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(vehiculoRepository.findById(vehiculo1.getId()).get().getActivo()).isFalse();

        // Actualizar estado a true
        updatedRows = vehiculoRepository.updateActivoStatus(vehiculo1.getId(), true);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(vehiculoRepository.findById(vehiculo1.getId()).get().getActivo()).isTrue();
    }
    
    @Test
    @DisplayName("Contar vehículos asignados a un conductor específico")
    void testCountByConductor() {
        long count = vehiculoRepository.countByConductor(conductor1);
        assertThat(count).isEqualTo(2); // vehiculo1 y vehiculo2
    }

    @Test
    @DisplayName("Buscar vehículos por un conductor específico")
    void testFindByConductor() {
        List<Vehiculo> vehiculosConductor1 = vehiculoRepository.findByConductor(conductor1);
        assertThat(vehiculosConductor1).isNotNull();
        assertThat(vehiculosConductor1).hasSize(2);
        assertThat(vehiculosConductor1.stream().allMatch(v -> v.getConductor().getId().equals(conductor1.getId()))).isTrue();
    }
}