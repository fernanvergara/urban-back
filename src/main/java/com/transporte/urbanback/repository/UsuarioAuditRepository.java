package com.transporte.urbanback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.security.UsuarioAudit;

@Repository
public interface UsuarioAuditRepository extends JpaRepository<UsuarioAudit, Long> {
    /**
     * Busca cambios en un usuario específico por su ID (el usuario auditado),
     * ordenado por fecha de cambio de forma descendente.
     * @param usuarioAuditadoId ID del usuario que fue auditado.
     * @return Lista de auditorías para el usuario auditado.
     */
    List<UsuarioAudit> findByUsuarioAuditadoIdOrderByFechaCambioDesc(Long usuarioAuditadoId);

    /**
     * Busca cambios realizados por un usuario específico por su ID (el usuario editor),
     * ordenado por fecha de cambio de forma descendente.
     * @param usuarioEditorId ID del usuario que realizó el cambio.
     * @return Lista de auditorías realizadas por el usuario editor.
     */
    List<UsuarioAudit> findByUsuarioEditorIdOrderByFechaCambioDesc(Long usuarioEditorId);

    /**
     * Lista cambios en un usuario específico por su username (el usuario auditado),
     * ordenado por fecha de cambio de forma descendente.
     * @param usernameAuditado Nombre de usuario del usuario auditado.
     * @return Lista de auditorías para el usuario auditado con el username especificado.
     */
    List<UsuarioAudit> findByUsuarioAuditadoUsernameOrderByFechaCambioDesc(String usernameAuditado);
    
    /**
     * Lista cambios realizados por un usuario específico por su username (el usuario editor),
     * ordenado por fecha de cambio de forma descendente.
     * @param usernameEditor Nombre de usuario del editor.
     * @return Lista de auditorías realizadas por el editor con el username especificado.
     */
    List<UsuarioAudit> findByUsuarioEditorUsernameOrderByFechaCambioDesc(String usernameEditor);
}