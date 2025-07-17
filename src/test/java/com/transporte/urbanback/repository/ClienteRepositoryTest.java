package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Usa la base de datos configurada, no una incrustada
@DisplayName("Tests para ClienteRepository")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente1;
    private Cliente cliente2;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll(); // Limpia la tabla antes de cada test

        cliente1 = new Cliente(
                null, // El ID será generado por la DB
                "Juan Pérez García",
                "1020304050",
                "+573001234567",
                "Calle Falsa 123, Barrio Imaginario",
                true // Activo
        );

        cliente2 = new Cliente(
                null,
                "María López",
                "1020304051",
                "+573007654321",
                "Avenida Siempre Viva 742, Springfield",
                false // Inactivo
        );

        // Guardar clientes para que estén disponibles en los tests
        cliente1 = clienteRepository.save(cliente1);
        cliente2 = clienteRepository.save(cliente2);
    }

    @Test
    @DisplayName("Guardar Cliente - Operación de Creación")
    void testSaveCliente() {
        Cliente nuevoCliente = new Cliente(
                null,
                "Pedro Gómez",
                "1020304052",
                "+573009876543",
                "Carrera 10 # 20-30, El Centro",
                true
        );
        Cliente clienteGuardado = clienteRepository.save(nuevoCliente);

        assertThat(clienteGuardado).isNotNull();
        assertThat(clienteGuardado.getId()).isNotNull();
        assertThat(clienteGuardado.getIdentificacion()).isEqualTo("1020304052");
    }

    @Test
    @DisplayName("Buscar Cliente por ID")
    void testFindById() {
        Optional<Cliente> foundCliente = clienteRepository.findById(cliente1.getId());

        assertThat(foundCliente).isPresent();
        assertThat(foundCliente.get().getIdentificacion()).isEqualTo("1020304050");
    }

    @Test
    @DisplayName("Buscar todos los Clientes")
    void testFindAllClientes() {
        List<Cliente> clientes = clienteRepository.findAll();
        assertThat(clientes).isNotNull();
        assertThat(clientes.size()).isEqualTo(2); // Deberían estar cliente1 y cliente2
    }

    @Test
    @DisplayName("Actualizar Cliente - Operación de Actualización")
    void testUpdateCliente() {
        Cliente clienteToUpdate = clienteRepository.findById(cliente1.getId()).get();
        clienteToUpdate.setTelefono("+573115555555");
        clienteToUpdate.setDireccionResidencia("Nueva Dirección 456");

        Cliente updatedCliente = clienteRepository.save(clienteToUpdate);

        assertThat(updatedCliente.getTelefono()).isEqualTo("+573115555555");
        assertThat(updatedCliente.getDireccionResidencia()).isEqualTo("Nueva Dirección 456");
    }

    @Test
    @DisplayName("Eliminar Cliente por ID - Operación de Eliminación")
    void testDeleteById() {
        clienteRepository.deleteById(cliente1.getId());
        Optional<Cliente> deletedCliente = clienteRepository.findById(cliente1.getId());
        assertThat(deletedCliente).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Cliente por Identificación")
    void testFindByIdentificacion() {
        Optional<Cliente> foundCliente = clienteRepository.findByIdentificacion("1020304050");
        assertThat(foundCliente).isPresent();
        assertThat(foundCliente.get().getNombreCompleto()).isEqualTo("Juan Pérez García");

        Optional<Cliente> notFoundCliente = clienteRepository.findByIdentificacion("9999999999");
        assertThat(notFoundCliente).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Clientes Activos")
    void testFindByActivoTrue() {
        List<Cliente> activos = clienteRepository.findByActivoTrue();
        assertThat(activos).isNotNull();
        assertThat(activos.size()).isEqualTo(1);
        assertThat(activos.get(0).getIdentificacion()).isEqualTo("1020304050");
    }

    @Test
    @DisplayName("Buscar Clientes Inactivos")
    void testFindByActivoFalse() {
        List<Cliente> inactivos = clienteRepository.findByActivoFalse();
        assertThat(inactivos).isNotNull();
        assertThat(inactivos.size()).isEqualTo(1);
        assertThat(inactivos.get(0).getIdentificacion()).isEqualTo("1020304051");
    }

    @Test
    @DisplayName("Actualizar estado activo de Cliente directamente con @Query")
    void testUpdateActivoStatus() {
        // Verificar estado inicial
        assertThat(clienteRepository.findById(cliente1.getId()).get().getActivo()).isTrue();

        // Actualizar estado a false
        int updatedRows = clienteRepository.updateActivoStatus(cliente1.getId(), false);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(clienteRepository.findById(cliente1.getId()).get().getActivo()).isFalse();

        // Actualizar estado a true
        updatedRows = clienteRepository.updateActivoStatus(cliente1.getId(), true);
        assertThat(updatedRows).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(clienteRepository.findById(cliente1.getId()).get().getActivo()).isTrue();
    }
}