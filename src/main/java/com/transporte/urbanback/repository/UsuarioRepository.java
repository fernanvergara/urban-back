package com.transporte.urbanback.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.transporte.urbanback.security.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /**
     * Busca un usuario por su nombre de usuario.
     * @param username Nombre de usuario a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío si no.
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica si ya existe un usuario con el nombre de usuario dado.
     * @param username Nombre de usuario a verificar.
     * @return true si el nombre de usuario ya existe, false en caso contrario.
     */
    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE Usuario u SET u.activo = :nuevoEstado WHERE u.id = :id")
    int updateActivoStatus(@Param("id") Long id, @Param("nuevoEstado") boolean nuevoEstado);
}
