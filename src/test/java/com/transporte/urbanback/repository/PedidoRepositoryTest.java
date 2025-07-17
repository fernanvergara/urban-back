package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Pedido;
import com.transporte.urbanback.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests para PedidoRepository")
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ConductorRepository conductorRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;

    private Cliente cliente1;
    private Conductor conductor1;
    private Vehiculo vehiculo1;
    private Pedido pedido1;
    private Pedido pedido2;
    private Pedido pedido3;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
        conductorRepository.deleteAll();
        vehiculoRepository.deleteAll();

        cliente1 = new Cliente(null, "Cliente Pedido", "1111111111", "+573001000000", "Dir Cliente P", true);
        cliente1 = clienteRepository.save(cliente1);

        conductor1 = new Conductor(null, "Conductor Pedido", "2222222222", LocalDate.of(1980,1,1), "+573002000000", true);
        conductor1 = conductorRepository.save(conductor1);

        vehiculo1 = new Vehiculo(null, "XYZ-789", new BigDecimal("1000.00"), "MarcaP", "ModeloP", 2020, true, conductor1);
        vehiculo1 = vehiculoRepository.save(vehiculo1);

        pedido1 = new Pedido(
                null,
                cliente1,
                "Origen Pedido 1",
                "Destino Pedido 1",
                LocalDateTime.now().minusHours(2), 
                LocalDateTime.now().minusHours(1).plusMinutes(30), 
                LocalDateTime.now().minusHours(1), 
                LocalDateTime.now().plusHours(1), 
                null, 
                EstadoPedido.EN_CAMINO,
                vehiculo1,
                conductor1,
                new BigDecimal("25.50"),
                ""
        );

        pedido2 = new Pedido(
                null,
                cliente1,
                "Origen Pedido 2",
                "Destino Pedido 2",
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(5).plusHours(1),
                null,
                LocalDateTime.now().minusDays(5).plusHours(2),
                null,
                EstadoPedido.PENDIENTE,
                null, 
                null,
                new BigDecimal("15.00"),
                ""
        );
        
        pedido3 = new Pedido(
                null,
                cliente1,
                "Origen Pedido 3",
                "Destino Pedido 3",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(1),
                LocalDateTime.now().minusDays(1).plusHours(1).plusMinutes(5),
                LocalDateTime.now().minusDays(1).plusHours(2),
                LocalDateTime.now().minusDays(1).plusHours(2).plusMinutes(10),
                EstadoPedido.COMPLETADO,
                vehiculo1,
                conductor1,
                new BigDecimal("40.00"),
                ""
        );


        pedido1 = pedidoRepository.save(pedido1);
        pedido2 = pedidoRepository.save(pedido2);
        pedido3 = pedidoRepository.save(pedido3);
    }

    @Test
    @DisplayName("Guardar Pedido - Operación de Creación")
    void testSavePedido() {
        Pedido nuevoPedido = new Pedido(
                null,
                cliente1,
                "Origen Nuevo",
                "Destino Nuevo",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                null,
                LocalDateTime.now().plusHours(2),
                null,
                EstadoPedido.PENDIENTE,
                null,
                null,
                new BigDecimal("30.00"),
                ""
        );
        Pedido pedidoGuardado = pedidoRepository.save(nuevoPedido);

        assertThat(pedidoGuardado).isNotNull();
        assertThat(pedidoGuardado.getId()).isNotNull();
        assertThat(pedidoGuardado.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    @DisplayName("Buscar Pedido por ID")
    void testFindById() {
        Optional<Pedido> foundPedido = pedidoRepository.findById(pedido1.getId());
        assertThat(foundPedido).isPresent();
        assertThat(foundPedido.get().getDireccionOrigen()).isEqualTo("Origen Pedido 1");
    }

    @Test
    @DisplayName("Buscar todos los Pedidos")
    void testFindAllPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        assertThat(pedidos).isNotNull();
        assertThat(pedidos.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Actualizar Pedido - Operación de Actualización")
    void testUpdatePedido() {
        Pedido pedidoToUpdate = pedidoRepository.findById(pedido1.getId()).get();
        pedidoToUpdate.setEstado(EstadoPedido.COMPLETADO);
        pedidoToUpdate.setFechaEntregaReal(LocalDateTime.now());

        Pedido updatedPedido = pedidoRepository.save(pedidoToUpdate);

        assertThat(updatedPedido.getEstado()).isEqualTo(EstadoPedido.COMPLETADO);
        assertThat(updatedPedido.getFechaEntregaReal()).isNotNull();
    }

    @Test
    @DisplayName("Eliminar Pedido por ID - Operación de Eliminación")
    void testDeleteById() {
        pedidoRepository.deleteById(pedido1.getId());
        Optional<Pedido> deletedPedido = pedidoRepository.findById(pedido1.getId());
        assertThat(deletedPedido).isNotPresent();
    }

    @Test
    @DisplayName("Buscar pedidos por el ID del cliente")
    void testFindByClienteId() {
        List<Pedido> pedidos = pedidoRepository.findByClienteId(cliente1.getId());
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(3); // Todos los pedidos son del cliente1
        assertThat(pedidos.stream().allMatch(p -> p.getCliente().getId().equals(cliente1.getId()))).isTrue();
    }

    @Test
    @DisplayName("Buscar pedidos por el ID del conductor")
    void testFindByConductorId() {
        List<Pedido> pedidos = pedidoRepository.findByConductorId(conductor1.getId());
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(2); // pedido1 y pedido3 tienen conductor1
        assertThat(pedidos.stream().allMatch(p -> p.getConductor() != null && p.getConductor().getId().equals(conductor1.getId()))).isTrue();
    }

    @Test
    @DisplayName("Buscar pedidos por el ID del vehículo")
    void testFindByVehiculoId() {
        List<Pedido> pedidos = pedidoRepository.findByVehiculoId(vehiculo1.getId());
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(2); // pedido1 y pedido3 tienen vehiculo1
        assertThat(pedidos.stream().allMatch(p -> p.getVehiculo() != null && p.getVehiculo().getId().equals(vehiculo1.getId()))).isTrue();
    }

    @Test
    @DisplayName("Buscar pedidos por estado PENDIENTE")
    void testFindByEstadoPendiente() {
        List<Pedido> pedidos = pedidoRepository.findByEstado(EstadoPedido.PENDIENTE);
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido2.getId());
    }
    
    @Test
    @DisplayName("Buscar pedidos por estado EN_CAMINO")
    void testFindByEstadoEnCamino() {
        List<Pedido> pedidos = pedidoRepository.findByEstado(EstadoPedido.EN_CAMINO);
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getEstado()).isEqualTo(EstadoPedido.EN_CAMINO);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido1.getId());
    }

    @Test
    @DisplayName("Buscar pedidos creados dentro de un rango de fechas")
    void testFindByFechaCreacionBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(6);
        LocalDateTime end = LocalDateTime.now().minusDays(4); // Solo debería incluir pedido2

        List<Pedido> pedidos = pedidoRepository.findByFechaCreacionBetween(start, end);
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido2.getId());

        start = LocalDateTime.now().minusHours(3);
        end = LocalDateTime.now().plusHours(1); // Debería incluir pedido1
        pedidos = pedidoRepository.findByFechaCreacionBetween(start, end);
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido1.getId());
    }

    @Test
    @DisplayName("Buscar pedidos por cliente y estado (Completados)")
    void testFindByClienteIdAndEstadoCompletado() {
        List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstado(cliente1.getId(), EstadoPedido.COMPLETADO);
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido3.getId());
    }

    @Test
    @DisplayName("Buscar pedidos por conductor y estado (En Camino)")
    void testFindByConductorIdAndEstadoEnCamino() {
        List<Pedido> pedidos = pedidoRepository.findByConductorIdAndEstado(conductor1.getId(), EstadoPedido.EN_CAMINO);
        assertThat(pedidos).isNotNull();
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getId()).isEqualTo(pedido1.getId());
    }
}