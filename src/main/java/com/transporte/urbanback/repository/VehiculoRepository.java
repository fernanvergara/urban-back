package com.transporte.urbanback.repository;

import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository // Opcional, pero buena práctica
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    /**
     * Busca un vehículo por su número de placa.
     * @param placa Número de placa del vehículo a buscar.
     * @return Un Optional que contiene el vehículo si se encuentra, o vacío si no.
     */
    Optional<Vehiculo> findByPlaca(String placa);

    /**
     * Busca todos los vehículos que están actualmente activos.
     * @return Una lista de vehículos activos.
     */
    List<Vehiculo> findByActivoTrue();

    /**
     * Busca todos los vehículos que están actualmente inactivos.
     * @return Una lista de vehículos inactivos.
     */
    List<Vehiculo> findByActivoFalse();

    /**
     * Actualiza directamente el estado 'activo' de un vehículo por su ID.
     * @param id ID del vehículo a actualizar.
     * @param nuevoEstado Nuevo estado (true para activo, false para inactivo).
     * @return El número de filas actualizadas.
     */
    @Modifying // Indica que esta consulta va a modificar datos
    @Query("UPDATE Vehiculo v SET v.activo = :nuevoEstado WHERE v.id = :id")
    int updateActivoStatus(@Param("id") Long id, @Param("nuevoEstado") Boolean nuevoEstado);

    /**
     * Cuenta el número de vehículos asignados a un conductor específico.
     * @param conductor El objeto Conductor.
     * @return El número de vehículos asignados a ese conductor.
     */
    long countByConductor(Conductor conductor);
    
    /**
     * Busca vehículos asignados a un conductor específico.
     * @param conductor El objeto Conductor.
     * @return Lista de vehículos asignados a ese conductor.
     */
    List<Vehiculo> findByConductor(Conductor conductor);
}