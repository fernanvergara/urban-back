package com.transporte.urbanback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.model.ConductorAudit;

@Repository
public interface ConductorAuditRepository extends JpaRepository<ConductorAudit, Long> {
    /**
     * Obtiene el historial de auditoría para un conductor específico por su ID,
     * ordenado por fecha de cambio de forma descendente.
     * @param conductorId ID del conductor.
     * @return Lista de auditorías para el conductor especificado.
     */
    List<ConductorAudit> findByConductorIdOrderByFechaCambioDesc(Long conductorId);

    /**
     * Obtiene el historial de auditoría para un conductor por su identificación,
     * ordenado por fecha de cambio de forma descendente.
     * @param identificacion Identificación del conductor.
     * @return Lista de auditorías para el conductor con la identificación especificada.
     */
    List<ConductorAudit> findByConductorIdentificacionOrderByFechaCambioDesc(String identificacion);

    /**
     * Obtiene el historial de auditoría para conductores cuyo nombre completo contiene la cadena dada,
     * ignorando mayúsculas y minúsculas, ordenado por fecha de cambio de forma descendente.
     * @param nombreCompleto Parte del nombre completo del conductor a buscar.
     * @return Lista de auditorías para conductores que coinciden con el nombre completo.
     */
    List<ConductorAudit> findByConductorNombreCompletoContainingIgnoreCaseOrderByFechaCambioDesc(String nombreCompleto);
}
