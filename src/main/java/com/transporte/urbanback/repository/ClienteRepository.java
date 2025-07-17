package com.transporte.urbanback.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.model.Cliente;

@Repository 
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    /**
     * Busca un cliente por su número de identificación (DNI/Cédula).
     * @param identificacion Número de identificación del cliente a buscar.
     * @return Un Optional que contiene el cliente si se encuentra, o vacío si no.
     */
    Optional<Cliente> findByIdentificacion(String identificacion);

    /**
     * Busca todos los clientes que están actualmente activos.
     * @return Una lista de clientes activos.
     */
    List<Cliente> findByActivoTrue();

    /**
     * Busca todos los clientes que están actualmente inactivos.
     * @return Una lista de clientes inactivos.
     */
    List<Cliente> findByActivoFalse();

    /**
     * Actualiza directamente el estado 'activo' de un cliente por su ID.
     * Utiliza @Modifying para indicar que es una consulta de modificación.
     * @param id ID del cliente a actualizar.
     * @param nuevoEstado Nuevo estado (true para activo, false para inactivo).
     * @return El número de filas actualizadas.
     */
    @Modifying
    @Query("UPDATE Cliente c SET c.activo = :nuevoEstado WHERE c.id = :id")
    int updateActivoStatus(@Param("id") Long id, @Param("nuevoEstado") Boolean nuevoEstado);
}
