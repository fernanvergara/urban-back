package com.transporte.urbanback.repository;

import com.transporte.urbanback.enums.EstadoPedido;
import com.transporte.urbanback.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca pedidos por el ID del cliente que los solicitó.
     * @param clienteId ID del cliente.
     * @return Lista de pedidos asociados a ese cliente.
     */
    List<Pedido> findByClienteId(Long clienteId);

    /**
     * Busca pedidos por el ID del conductor asignado.
     * @param conductorId ID del conductor.
     * @return Lista de pedidos asignados a ese conductor.
     */
    List<Pedido> findByConductorId(Long conductorId);

    /**
     * Busca pedidos por el ID del vehículo asignado.
     * @param vehiculoId ID del vehículo.
     * @return Lista de pedidos asociados a ese vehículo.
     */
    List<Pedido> findByVehiculoId(Long vehiculoId);

    /**
     * Busca pedidos por su estado.
     * @param estado Estado del pedido (ej. PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO).
     * @return Lista de pedidos con el estado especificado.
     */
    List<Pedido> findByEstado(EstadoPedido estado); 

    /**
     * Busca pedidos creados dentro de un rango de fechas.
     * @param fechaInicio Fecha y hora de inicio del rango (inclusive).
     * @param fechaFin Fecha y hora de fin del rango (inclusive).
     * @return Lista de pedidos creados en el rango especificado.
     */
    List<Pedido> findByFechaCreacionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Busca pedidos por cliente y estado.
     * @param clienteId ID del cliente.
     * @param estado Estado del pedido.
     * @return Lista de pedidos de un cliente con un estado específico.
     */
    List<Pedido> findByClienteIdAndEstado(Long clienteId, EstadoPedido estado);

    /**
     * Busca pedidos por conductor y estado.
     * @param conductorId ID del conductor.
     * @param estado Estado del pedido.
     * @return Lista de pedidos de un conductor con un estado específico.
     */
    List<Pedido> findByConductorIdAndEstado(Long conductorId, EstadoPedido estado);
}