package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.PedidoAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoAuditRepository extends JpaRepository<PedidoAudit, Long> {

    /**
     * Obtiene el historial de auditoría para un pedido específico, ordenado por fecha de cambio descendente.
     * @param pedidoId El ID del pedido.
     * @return Una lista de registros de auditoría para el pedido.
     */
    List<PedidoAudit> findByPedidoIdOrderByFechaCambioDesc(Long pedidoId);

    /**
     * Obtiene el historial de auditoría de pedidos filtrado por el ID del usuario editor,
     * ordenado por fecha de cambio descendente.
     * @param usuarioEditorId El ID del usuario que realizó la operación de auditoría.
     * @return Una lista de registros de auditoría realizados por el usuario editor.
     */
    List<PedidoAudit> findByUsuarioEditorIdOrderByFechaCambioDesc(Long usuarioEditorId);

    /**
     * Obtiene el historial de auditoría de pedidos filtrado por el nombre de usuario del editor,
     * ordenado por fecha de cambio descendente.
     * @param usernameEditor El nombre de usuario del editor que realizó la operación de auditoría.
     * @return Una lista de registros de auditoría realizados por el usuario editor.
     */
    List<PedidoAudit> findByUsuarioEditorUsernameOrderByFechaCambioDesc(String usernameEditor);
}