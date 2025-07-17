package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.Conductor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    /**
     * Busca un conductor por su número de identificación.
     * @param identificacion Número de identificación del conductor a buscar.
     * @return Un Optional que contiene el conductor si se encuentra, o vacío si no.
     */
    Optional<Conductor> findByIdentificacion(String identificacion);

    /**
     * Busca todos los conductores que están actualmente activos.
     * @return Una lista de conductores activos.
     */
    List<Conductor> findByActivoTrue();

    /**
     * Busca todos los conductores que están actualmente inactivos.
     * @return Una lista de conductores inactivos.
     */
    List<Conductor> findByActivoFalse();

    /**
     * Actualiza directamente el estado 'activo' de un conductor por su ID.
     * Utiliza @Modifying para indicar que es una consulta de modificación.
     * @param id ID del conductor a actualizar.
     * @param nuevoEstado Nuevo estado (true para activo, false para inactivo).
     * @return El número de filas actualizadas.
     */
    @Modifying
    @Query("UPDATE Conductor c SET c.activo = :nuevoEstado WHERE c.id = :id")
    int updateActivoStatus(@Param("id") Long id, @Param("nuevoEstado") Boolean nuevoEstado);

}