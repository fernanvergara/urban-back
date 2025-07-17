package com.transporte.urbanback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.model.VehiculoAudit;

@Repository
public interface VehiculoAuditRepository extends JpaRepository<VehiculoAudit, Long> {
    /**
     * Busca el historial de auditoría para un vehículo específico por su ID,
     * ordenado por fecha de cambio de forma descendente.
     * @param vehiculoId ID del vehículo.
     * @return Lista de auditorías para el vehículo especificado.
     */
    List<VehiculoAudit> findByVehiculoIdOrderByFechaCambioDesc(Long vehiculoId);

    /**
     * Busca el historial de auditoría para un vehículo por su placa,
     * ordenado por fecha de cambio de forma descendente.
     * @param placa Placa del vehículo.
     * @return Lista de auditorías para el vehículo con la placa especificada.
     */
    List<VehiculoAudit> findByVehiculoPlacaOrderByFechaCambioDesc(String placa);
}
