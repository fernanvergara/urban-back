package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.security.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para UsuarioRepository")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository; // Para relacionar con Usuario
    @Autowired
    private ConductorRepository conductorRepository; // Para relacionar con Usuario

    private Usuario adminUser;
    private Usuario conductorUser;
    private Usuario clientUser;
    private Cliente testClient;
    private Conductor testConductor;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        conductorRepository.deleteAll();

        // Crear entidades relacionadas (Cliente y Conductor)
        testClient = new Cliente(null, "Test Client", "CLT123", "+573000000001", "Client Address", true);
        testClient = clienteRepository.save(testClient);

        testConductor = new Conductor(null, "Test Conductor", "CND456", LocalDate.of(1990, 1, 1), "+573000000002", true);
        testConductor = conductorRepository.save(testConductor);

        adminUser = new Usuario(null, "admin.test", "password_hash_admin", Rol.ADMIN, null, null, true);
        conductorUser = new Usuario(null, "conductor.test", "password_hash_conductor", Rol.CONDUCTOR, testConductor, null, true);
        clientUser = new Usuario(null, "client.test", "password_hash_client", Rol.CLIENTE, null, testClient, true);

        adminUser = usuarioRepository.save(adminUser);
        conductorUser = usuarioRepository.save(conductorUser);
        clientUser = usuarioRepository.save(clientUser);
    }

    @Test
    @DisplayName("Guardar Usuario - Operación de Creación")
    void testSaveUsuario() {
        Usuario newUser = new Usuario(null, "new.user", "new_password", Rol.CLIENTE, null, null, true);
        Usuario savedUser = usuarioRepository.save(newUser);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("new.user");
        assertThat(savedUser.getRol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("Buscar Usuario por ID")
    void testFindById() {
        Optional<Usuario> foundUser = usuarioRepository.findById(adminUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("admin.test");
    }

    @Test
    @DisplayName("Buscar todos los Usuarios")
    void testFindAllUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        assertThat(usuarios).isNotNull();
        assertThat(usuarios.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Actualizar Usuario - Operación de Actualización")
    void testUpdateUsuario() {
        Usuario userToUpdate = usuarioRepository.findById(adminUser.getId()).get();
        userToUpdate.setActivo(false); // Cambiar a inactivo

        Usuario updatedUser = usuarioRepository.save(userToUpdate);

        assertThat(updatedUser.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Eliminar Usuario por ID - Operación de Eliminación")
    void testDeleteById() {
        usuarioRepository.deleteById(adminUser.getId());
        Optional<Usuario> deletedUser = usuarioRepository.findById(adminUser.getId());
        assertThat(deletedUser).isNotPresent();
    }

    @Test
    @DisplayName("Buscar Usuario por Username")
    void testFindByUsername() {
        Optional<Usuario> foundUser = usuarioRepository.findByUsername("conductor.test");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRol()).isEqualTo(Rol.CONDUCTOR);
        assertThat(foundUser.get().getConductor()).isEqualTo(testConductor); // Verificar relación

        Optional<Usuario> notFoundUser = usuarioRepository.findByUsername("nonexistent");
        assertThat(notFoundUser).isNotPresent();
    }

    @Test
    @DisplayName("Verificar si existe un Usuario por Username")
    void testExistsByUsername() {
        assertThat(usuarioRepository.existsByUsername("client.test")).isTrue();
        assertThat(usuarioRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Verificar relaciones OneToOne con Cliente y Conductor")
    void testOneToOneRelationships() {
        // Conductor User
        Optional<Usuario> foundConductorUser = usuarioRepository.findById(conductorUser.getId());
        assertThat(foundConductorUser).isPresent();
        assertThat(foundConductorUser.get().getConductor()).isNotNull();
        assertThat(foundConductorUser.get().getConductor().getIdentificacion()).isEqualTo("CND456");
        assertThat(foundConductorUser.get().getCliente()).isNull(); // No debe tener cliente

        // Client User
        Optional<Usuario> foundClientUser = usuarioRepository.findById(clientUser.getId());
        assertThat(foundClientUser).isPresent();
        assertThat(foundClientUser.get().getCliente()).isNotNull();
        assertThat(foundClientUser.get().getCliente().getIdentificacion()).isEqualTo("CLT123");
        assertThat(foundClientUser.get().getConductor()).isNull(); // No debe tener conductor

        // Admin User (no asociado a Cliente ni Conductor)
        Optional<Usuario> foundAdminUser = usuarioRepository.findById(adminUser.getId());
        assertThat(foundAdminUser).isPresent();
        assertThat(foundAdminUser.get().getCliente()).isNull();
        assertThat(foundAdminUser.get().getConductor()).isNull();
    }
}