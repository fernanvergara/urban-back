package com.transporte.urbanback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.model.ClienteAudit;

@Repository
public interface ClienteAuditRepository extends JpaRepository<ClienteAudit, Long> {
    /**
     * Obtener el historial de auditoría para un cliente específico por su ID,
     * ordenado por fecha de cambio de forma descendente.
     * @param clienteId ID del cliente.
     * @return Lista de auditorías para el cliente especificado.
     */
    List<ClienteAudit> findByClienteIdOrderByFechaCambioDesc(Long clienteId);

    /**
     * Obtener el historial de auditoría para un cliente por su identificación,
     * ordenado por fecha de cambio de forma descendente.
     * @param identificacion Identificación del cliente.
     * @return Lista de auditorías para el cliente con la identificación especificada.
     */
    List<ClienteAudit> findByClienteIdentificacionOrderByFechaCambioDesc(String identificacion);

    /**
     * Obtener historial de auditoría de cambios realizados por un usuario editor por su username,
     * ordenado por fecha de cambio de forma descendente.
     * @param usernameEditor Nombre de usuario del editor.
     * @return Lista de auditorías realizadas por el usuario editor especificado.
     */
    List<ClienteAudit> findByUsuarioEditorUsernameOrderByFechaCambioDesc(String usernameEditor);
}
