// src/main/java/com/transporte/urbanback/security/SecurityUtils.java

package com.transporte.urbanback.security;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.repository.VehiculoRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("securityUtils") // El nombre del bean para usar en SpEL: @securityUtils
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;
    private final VehiculoRepository vehiculoRepository;

    public SecurityUtils(UsuarioRepository usuarioRepository, VehiculoRepository vehiculoRepository) { 
        this.usuarioRepository = usuarioRepository;
        this.vehiculoRepository = vehiculoRepository; 
    }

    /**
     * Método para obtener el usuario actualmente autenticado del contexto de seguridad de Spring.
     * Retorna un Optional vacío si no hay un usuario autenticado, si la autenticación no es válida,
     * o si el principal es un usuario anónimo o de tipo desconocido.
     *
     * @return Un Optional que contiene el objeto Usuario si está autenticado y se encuentra, o un Optional vacío.
     */
    private Optional<Usuario> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        String nombreUsuario = "";
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            nombreUsuario = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            nombreUsuario = (String) principal;
        } else {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(nombreUsuario);
    }

    /**
     * Verifica si el ID del conductor dado está vinculado al usuario actualmente autenticado (si es CONDUCTOR).
     * @param conductorId El ID del conductor a verificar.
     * @return true si el conductor pertenece al usuario autenticado, false en caso contrario.
     */
    public boolean isConductorIdLinkedToCurrentUser(Long conductorId) {
        Optional<Usuario> currentUser = getCurrentUser();
        return currentUser.map(user ->
            user.getRol() == Rol.CONDUCTOR && 
            user.getConductor() != null && 
            user.getConductor().getId().equals(conductorId) 
        ).orElse(false);
    }

    /**
     * Verifica si la identificación del conductor dado está vinculada al usuario actualmente autenticado (si es CONDUCTOR).
     * @param identificacion La identificación del conductor a verificar.
     * @return true si la identificación del conductor coincide con la del usuario autenticado, false en caso contrario.
     */
    public boolean isConductorIdentificacionLinkedToCurrentUser(String identificacion) {
        Optional<Usuario> currentUser = getCurrentUser();
        return currentUser.map(user ->
            user.getRol() == Rol.CONDUCTOR &&
            user.getConductor() != null &&
            user.getConductor().getIdentificacion().equals(identificacion)
        ).orElse(false);
    }

    /*Opcional
    public boolean isConductorNombreLinkedToCurrentUser(String nombre) {
        Optional<Usuario> currentUser = getCurrentUser();
        return currentUser.map(user ->
            user.getRol() == Rol.CONDUCTOR &&
            user.getConductor() != null &&
            user.getConductor().getNombre().equals(nombre)
        ).orElse(false);
    }
    */

    /**
     * Verifica si un vehículo con el ID dado está asignado al conductor actual.
     * @param vehiculoId El ID del vehículo a verificar.
     * @return true si el vehículo está asignado al conductor autenticado, false en caso contrario.
     */
    public boolean isVehiculoIdAssignedToCurrentUser(Long vehiculoId) {
        Optional<Usuario> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty() || currentUserOpt.get().getRol() != Rol.CONDUCTOR) {
            return false; // No hay usuario o no es conductor
        }
        Usuario currentUser = currentUserOpt.get();
        if (currentUser.getConductor() == null) {
            return false; // El usuario CONDUCTOR no tiene un perfil de Conductor asociado
        }

        Optional<Vehiculo> vehiculoOpt = vehiculoRepository.findById(vehiculoId);
        return vehiculoOpt.map(vehiculo ->
            vehiculo.getConductor() != null && 
            vehiculo.getConductor().getId().equals(currentUser.getConductor().getId()) 
        ).orElse(false);
    }

    /**
     * Verifica si un vehículo con la placa dada está asignado al conductor actual.
     * @param placa La placa del vehículo a verificar.
     * @return true si el vehículo está asignado al conductor autenticado, false en caso contrario.
     */
    public boolean isVehiculoPlacaAssignedToCurrentUser(String placa) {
        Optional<Usuario> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty() || currentUserOpt.get().getRol() != Rol.CONDUCTOR) {
            return false;
        }
        Usuario currentUser = currentUserOpt.get();
        if (currentUser.getConductor() == null) {
            return false;
        }

        Optional<Vehiculo> vehiculoOpt = vehiculoRepository.findByPlaca(placa); 
        return vehiculoOpt.map(vehiculo ->
            vehiculo.getConductor() != null &&
            vehiculo.getConductor().getId().equals(currentUser.getConductor().getId())
        ).orElse(false);
    }
    
    /**
     * Verifica si el Cliente con el ID dado está asociado al Usuario actualmente autenticado.
     * Solo para usuarios con rol CLIENTE.
     *
     * @param clienteId El ID del cliente a verificar.
     * @return true si el cliente pertenece al usuario autenticado, false en caso contrario.
     */
    public boolean isClienteOwnedByCurrentUser(Long clienteId) {
        Optional<Usuario> currentUser = getCurrentUser();
        return currentUser.map(user ->
            user.getRol() == Rol.CLIENTE && 
            user.getCliente() != null && 
            user.getCliente().getId().equals(clienteId) 
        ).orElse(false);
    }

    /**
     * Obtiene el nombre de usuario autenticado del contexto de seguridad de Spring.
     * Si no hay un usuario autenticado o el principal no es una instancia de UserDetails,
     * retorna un valor por defecto ("system_anonymous") para indicar que no se pudo determinar el usuario.
     *
     * @return El nombre de usuario autenticado o "system_anonymous" si no está disponible.
     */
    public String obtenerNombreUsuarioAutenticado() {
        Optional<Usuario> currentUser = getCurrentUser();
        if (currentUser.isPresent() ) {
            return currentUser.get().getUsername();
        }
        return "usuario_desconocido"; // Valor por defecto para usuarios no autenticados o no identificados.
    }

}