package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.Conductor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) 
@DisplayName("Tests para ConductorRepository")
class ConductorRepositoryTest {

    @Autowired
    private ConductorRepository conductorRepository;
    @Autowired
    private TestEntityManager entityManager;

    private Conductor conductor1;
    private Conductor conductor2;

    @BeforeEach
    void setUp() {
        conductorRepository.deleteAll(); // Limpia la tabla antes de cada test

        conductor1 = new Conductor(
                null,
                "Carlos Andrés Restrepo",
                "1122334455",
                LocalDate.of(1985, 5, 15),
                "+573101234567",
                true // Activo
        );

        conductor2 = new Conductor(
                null,
                "Ana María Puerta",
                "9988776655",
                LocalDate.of(1990, 10, 20),
                "+573209876543",
                false // Inactivo
        );

        conductor1 = conductorRepository.save(conductor1);
        conductor2 = conductorRepository.save(conductor2);
    }

    @Test
    @DisplayName("Guardar Conductor - Operación de Creación")
    void testSaveConductor() {
        Conductor nuevoConductor = new Conductor(
                null,
                "Laura Victoria Soto",
                "1112223334",
                LocalDate.of(1992, 1, 1),
                "+573151112233",
                true
        );
        Conductor conductorGuardado = conductorRepository.save(nuevoConductor);

        assertThat(conductorGuardado).isNotNull();
        assertThat(conductorGuardado.getId()).isNotNull();
        assertThat(conductorGuardado.getIdentificacion()).isEqualTo("1112223334");
    }

    @Test
    @DisplayName("Buscar Conductor por ID")
    void testFindById() {
        Optional<Conductor> foundConductor = conductorRepository.findById(conductor1.getId());
        assertThat(foundConductor).isPresent();
        assertThat(foundConductor.get().getIdentificacion()).isEqualTo("1122334455");
    }

    @Test
    @DisplayName("Buscar todos los Conductores")
    void testFindAllConductors() {
        List<Conductor> conductores = conductorRepository.findAll();
        assertThat(conductores).isNotNull();
        assertThat(conductores.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Actualizar Conductor - Operación de Actualización")
    void testUpdateConductor() {
        Conductor conductorToUpdate = conductorRepository.findById(conductor1.getId()).get();
        conductorToUpdate.setTelefono("+573100000000");

        Conductor updatedConductor = conductorRepository.save(conductorToUpdate);

        assertThat(updatedConductor.getTelefono()).isEqualTo("+573100000000");
    }

    @Test
    @DisplayName("Eliminar Conductor por ID - Operación de Eliminación")
    void testDeleteById() {
        conductorRepository.deleteById(conductor1.getId());
        Optional<Conductor> deletedConductor = conductorRepository.findById(conductor1.getId());
        assertThat(deletedConductor).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Conductor por Identificación")
    void testFindByIdentificacion() {
        Optional<Conductor> foundConductor = conductorRepository.findByIdentificacion("1122334455");
        assertThat(foundConductor).isPresent();
        assertThat(foundConductor.get().getNombreCompleto()).isEqualTo("Carlos Andrés Restrepo");

        Optional<Conductor> notFoundConductor = conductorRepository.findByIdentificacion("0000000000");
        assertThat(notFoundConductor).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Conductores Activos")
    void testFindByActivoTrue() {
        List<Conductor> activos = conductorRepository.findByActivoTrue();
        assertThat(activos).isNotNull();
        assertThat(activos.size()).isEqualTo(1);
        assertThat(activos.get(0).getIdentificacion()).isEqualTo("1122334455");
    }

    @Test
    @DisplayName("Buscar Conductores Inactivos")
    void testFindByActivoFalse() {
        List<Conductor> inactivos = conductorRepository.findByActivoFalse();
        assertThat(inactivos).isNotNull();
        assertThat(inactivos.size()).isEqualTo(1);
        assertThat(inactivos.get(0).getIdentificacion()).isEqualTo("9988776655");
    }

    @Test
    @DisplayName("Actualizar estado activo de Conductor directamente con @Query")
    void testUpdateActivoStatus() {
        // Verificar estado inicial
        assertThat(conductorRepository.findById(conductor1.getId()).get().getActivo()).isTrue();

        // Actualizar estado a false
        int updatedRows = conductorRepository.updateActivoStatus(conductor1.getId(), false);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(conductorRepository.findById(conductor1.getId()).get().getActivo()).isFalse();

        // Actualizar estado a true
        updatedRows = conductorRepository.updateActivoStatus(conductor1.getId(), true);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(conductorRepository.findById(conductor1.getId()).get().getActivo()).isTrue();
    }
}