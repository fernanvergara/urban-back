package com.transporte.urbanback.security;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.model.Vehiculo;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.repository.VehiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Clase de pruebas para SecurityUtils.
 * Utiliza Mockito para simular las dependencias y Mockito.mockStatic para
 * probar interacciones con SecurityContextHolder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para SecurityUtils")
class SecurityUtilsTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    private SecurityContext securityContext;
    private Authentication authentication;

    private Usuario adminUser;
    private Usuario conductorUser;
    private Conductor conductorProfile;
    private Usuario clienteUser;
    private Cliente clienteProfile;
    private Vehiculo vehiculoConductor1;

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);

        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        adminUser = new Usuario(1L, "adminuser", "pass", Rol.ADMIN, null, null, true);
        
        conductorProfile = new Conductor(10L, "Conductor Test", "12345", LocalDate.now(), "1234567890", true);
        conductorUser = new Usuario(2L, "driveruser", "pass", Rol.CONDUCTOR, conductorProfile, null, true);

        clienteProfile = new Cliente(20L, "Cliente Test", "67890", "111222333", "Dir Cliente", true);
        clienteUser = new Usuario(3L, "clientuser", "pass", Rol.CLIENTE, null, clienteProfile, true);

        vehiculoConductor1 = new Vehiculo(100L, "ABC-123", null, "Marca", "Modelo", 2020, true, conductorProfile);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    /**
     * Test para verificar isConductorIdLinkedToCurrentUser cuando el usuario es CONDUCTOR y el ID coincide.
     */
    @Test
    @DisplayName("isConductorIdLinkedToCurrentUser debe retornar true para CONDUCTOR con ID coincidente")
    void isConductorIdLinkedToCurrentUser_whenConductorAndIdMatches_thenReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));

        assertTrue(securityUtils.isConductorIdLinkedToCurrentUser(conductorProfile.getId()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
    }

    /**
     * Test para verificar isConductorIdLinkedToCurrentUser cuando el usuario es CONDUCTOR pero el ID no coincide.
     */
    @Test
    @DisplayName("isConductorIdLinkedToCurrentUser debe retornar false para CONDUCTOR con ID no coincidente")
    void isConductorIdLinkedToCurrentUser_whenConductorAndIdNotMatches_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));

        assertFalse(securityUtils.isConductorIdLinkedToCurrentUser(999L));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
    }

    /**
     * Test para verificar isConductorIdLinkedToCurrentUser cuando el usuario no es CONDUCTOR.
     */
    @Test
    @DisplayName("isConductorIdLinkedToCurrentUser debe retornar false si el usuario no es CONDUCTOR")
    void isConductorIdLinkedToCurrentUser_whenUserNotConductor_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(adminUser.getUsername(), adminUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(adminUser.getUsername())).thenReturn(Optional.of(adminUser));

        assertFalse(securityUtils.isConductorIdLinkedToCurrentUser(conductorProfile.getId()));
        verify(usuarioRepository, times(1)).findByUsername(adminUser.getUsername());
    }

    /**
     * Test para verificar isConductorIdLinkedToCurrentUser cuando no hay usuario autenticado.
     */
    @Test
    @DisplayName("isConductorIdLinkedToCurrentUser debe retornar false si no hay usuario autenticado")
    void isConductorIdLinkedToCurrentUser_whenNoUserAuthenticated_thenReturnFalse() {
        lenient().when(authentication.isAuthenticated()).thenReturn(false); // Marcar como lenient
        lenient().when(securityContext.getAuthentication()).thenReturn(null); // Marcar como lenient

        assertFalse(securityUtils.isConductorIdLinkedToCurrentUser(conductorProfile.getId()));
        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    /**
     * Test para verificar isConductorIdentificacionLinkedToCurrentUser cuando el usuario es CONDUCTOR y la identificación coincide.
     */
    @Test
    @DisplayName("isConductorIdentificacionLinkedToCurrentUser debe retornar true para CONDUCTOR con identificación coincidente")
    void isConductorIdentificacionLinkedToCurrentUser_whenConductorAndIdentificacionMatches_thenReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));

        assertTrue(securityUtils.isConductorIdentificacionLinkedToCurrentUser(conductorProfile.getIdentificacion()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
    }

    /**
     * Test para verificar isConductorIdentificacionLinkedToCurrentUser cuando el usuario es CONDUCTOR pero la identificación no coincide.
     */
    @Test
    @DisplayName("isConductorIdentificacionLinkedToCurrentUser debe retornar false para CONDUCTOR con identificación no coincidente")
    void isConductorIdentificacionLinkedToCurrentUser_whenConductorAndIdentificacionNotMatches_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));

        assertFalse(securityUtils.isConductorIdentificacionLinkedToCurrentUser("99999"));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
    }

    /**
     * Test para verificar isVehiculoIdAssignedToCurrentUser cuando el vehículo está asignado al conductor actual.
     */
    @Test
    @DisplayName("isVehiculoIdAssignedToCurrentUser debe retornar true si el vehículo está asignado al CONDUCTOR actual")
    void isVehiculoIdAssignedToCurrentUser_whenVehiculoAssignedToConductor_thenReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findById(vehiculoConductor1.getId())).thenReturn(Optional.of(vehiculoConductor1));

        assertTrue(securityUtils.isVehiculoIdAssignedToCurrentUser(vehiculoConductor1.getId()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findById(vehiculoConductor1.getId());
    }

    /**
     * Test para verificar isVehiculoIdAssignedToCurrentUser cuando el vehículo no está asignado al conductor actual.
     */
    @Test
    @DisplayName("isVehiculoIdAssignedToCurrentUser debe retornar false si el vehículo no está asignado al CONDUCTOR actual")
    void isVehiculoIdAssignedToCurrentUser_whenVehiculoNotAssignedToConductor_thenReturnFalse() {
        Vehiculo otherVehiculo = new Vehiculo(200L, "DEF-456", null, "Otro", "Modelo", 2021, true, new Conductor(99L, "Otro Conductor", "54321", LocalDate.now(), "0987654321", true));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findById(otherVehiculo.getId())).thenReturn(Optional.of(otherVehiculo));

        assertFalse(securityUtils.isVehiculoIdAssignedToCurrentUser(otherVehiculo.getId()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findById(otherVehiculo.getId());
    }

    /**
     * Test para verificar isVehiculoIdAssignedToCurrentUser cuando el vehículo no existe.
     */
    @Test
    @DisplayName("isVehiculoIdAssignedToCurrentUser debe retornar false si el vehículo no existe")
    void isVehiculoIdAssignedToCurrentUser_whenVehiculoNotFound_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertFalse(securityUtils.isVehiculoIdAssignedToCurrentUser(999L));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findById(anyLong());
    }

    /**
     * Test para verificar isVehiculoPlacaAssignedToCurrentUser cuando la placa coincide.
     */
    @Test
    @DisplayName("isVehiculoPlacaAssignedToCurrentUser debe retornar true si la placa está asignada al CONDUCTOR actual")
    void isVehiculoPlacaAssignedToCurrentUser_whenPlacaAssignedToConductor_thenReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findByPlaca(vehiculoConductor1.getPlaca())).thenReturn(Optional.of(vehiculoConductor1));

        assertTrue(securityUtils.isVehiculoPlacaAssignedToCurrentUser(vehiculoConductor1.getPlaca()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findByPlaca(vehiculoConductor1.getPlaca());
    }

    /**
     * Test para verificar isVehiculoPlacaAssignedToCurrentUser cuando la placa no coincide.
     */
    @Test
    @DisplayName("isVehiculoPlacaAssignedToCurrentUser debe retornar false si la placa no está asignada al CONDUCTOR actual")
    void isVehiculoPlacaAssignedToCurrentUser_whenPlacaNotAssignedToConductor_thenReturnFalse() {
        Vehiculo otherVehiculo = new Vehiculo(200L, "DEF-456", null, "Otro", "Modelo", 2021, true, new Conductor(99L, "Otro Conductor", "54321", LocalDate.now(), "0987654321", true));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findByPlaca(otherVehiculo.getPlaca())).thenReturn(Optional.of(otherVehiculo));

        assertFalse(securityUtils.isVehiculoPlacaAssignedToCurrentUser(otherVehiculo.getPlaca()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findByPlaca(otherVehiculo.getPlaca());
    }

    /**
     * Test para verificar isVehiculoPlacaAssignedToCurrentUser cuando la placa no existe.
     */
    @Test
    @DisplayName("isVehiculoPlacaAssignedToCurrentUser debe retornar false si la placa no existe")
    void isVehiculoPlacaAssignedToCurrentUser_whenPlacaNotFound_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUser.getUsername(), conductorUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUser.getUsername())).thenReturn(Optional.of(conductorUser));
        when(vehiculoRepository.findByPlaca(anyString())).thenReturn(Optional.empty());

        assertFalse(securityUtils.isVehiculoPlacaAssignedToCurrentUser("NON-EXISTENT"));
        verify(usuarioRepository, times(1)).findByUsername(conductorUser.getUsername());
        verify(vehiculoRepository, times(1)).findByPlaca(anyString());
    }

    /**
     * Test para verificar isClienteOwnedByCurrentUser cuando el usuario es CLIENTE y el ID coincide.
     */
    @Test
    @DisplayName("isClienteOwnedByCurrentUser debe retornar true para CLIENTE con ID coincidente")
    void isClienteOwnedByCurrentUser_whenClienteAndIdMatches_thenReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(clienteUser.getUsername(), clienteUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(clienteUser.getUsername())).thenReturn(Optional.of(clienteUser));

        assertTrue(securityUtils.isClienteOwnedByCurrentUser(clienteProfile.getId()));
        verify(usuarioRepository, times(1)).findByUsername(clienteUser.getUsername());
    }

    /**
     * Test para verificar isClienteOwnedByCurrentUser cuando el usuario es CLIENTE pero el ID no coincide.
     */
    @Test
    @DisplayName("isClienteOwnedByCurrentUser debe retornar false para CLIENTE con ID no coincidente")
    void isClienteOwnedByCurrentUser_whenClienteAndIdNotMatches_thenReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(clienteUser.getUsername(), clienteUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(clienteUser.getUsername())).thenReturn(Optional.of(clienteUser));

        assertFalse(securityUtils.isClienteOwnedByCurrentUser(999L));
        verify(usuarioRepository, times(1)).findByUsername(clienteUser.getUsername());
    }

    /**
     * Test para verificar obtenerNombreUsuarioAutenticado cuando hay un usuario autenticado.
     */
    @Test
    @DisplayName("obtenerNombreUsuarioAutenticado debe retornar el nombre de usuario autenticado")
    void obtenerNombreUsuarioAutenticado_whenAuthenticatedUser_thenReturnUsername() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(adminUser.getUsername(), adminUser.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(adminUser.getUsername())).thenReturn(Optional.of(adminUser));

        String username = securityUtils.obtenerNombreUsuarioAutenticado();

        assertEquals(adminUser.getUsername(), username);
        verify(usuarioRepository, times(1)).findByUsername(adminUser.getUsername());
    }

    /**
     * Test para verificar obtenerNombreUsuarioAutenticado cuando no hay usuario autenticado.
     */
    @Test
    @DisplayName("obtenerNombreUsuarioAutenticado debe retornar 'usuario_desconocido' si no hay usuario autenticado")
    void obtenerNombreUsuarioAutenticado_whenNoAuthenticatedUser_thenReturnDefault() {
        lenient().when(authentication.isAuthenticated()).thenReturn(false);
        lenient().when(securityContext.getAuthentication()).thenReturn(null);

        String username = securityUtils.obtenerNombreUsuarioAutenticado();

        assertEquals("usuario_desconocido", username);
        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    /**
     * Test para verificar obtenerNombreUsuarioAutenticado cuando el principal es un String (ej. anónimo).
     */
    @Test
    @DisplayName("obtenerNombreUsuarioAutenticado debe retornar 'usuario_desconocido' si el principal es anónimo")
    void obtenerNombreUsuarioAutenticado_whenPrincipalIsAnonymousString_thenReturnDefault() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        // REMOVIDO: No se debe mockear findByUsername("anonymousUser") ya que el método getCurrentUser()
        // retorna Optional.empty() directamente si el principal es "anonymousUser".
        // when(usuarioRepository.findByUsername("anonymousUser")).thenReturn(Optional.empty());

        String username = securityUtils.obtenerNombreUsuarioAutenticado();

        assertEquals("usuario_desconocido", username);
        // VERIFICACIÓN: Asegurarse de que findByUsername NUNCA se llama para "anonymousUser"
        verify(usuarioRepository, never()).findByUsername("anonymousUser");
    }

    /**
     * Test para verificar isConductorIdLinkedToCurrentUser cuando el usuario CONDUCTOR no tiene perfil de conductor.
     */
    @Test
    @DisplayName("isConductorIdLinkedToCurrentUser debe retornar false si el usuario CONDUCTOR no tiene perfil de conductor")
    void isConductorIdLinkedToCurrentUser_whenConductorUserNoProfile_thenReturnFalse() {
        Usuario conductorUserNoProfile = new Usuario(2L, "driveruser", "pass", Rol.CONDUCTOR, null, null, true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUserNoProfile.getUsername(), conductorUserNoProfile.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUserNoProfile.getUsername())).thenReturn(Optional.of(conductorUserNoProfile));

        assertFalse(securityUtils.isConductorIdLinkedToCurrentUser(conductorProfile.getId()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUserNoProfile.getUsername());
    }

    /**
     * Test para verificar isConductorIdentificacionLinkedToCurrentUser cuando el usuario CONDUCTOR no tiene perfil de conductor.
     */
    @Test
    @DisplayName("isConductorIdentificacionLinkedToCurrentUser debe retornar false si el usuario CONDUCTOR no tiene perfil de conductor")
    void isConductorIdentificacionLinkedToCurrentUser_whenConductorUserNoProfile_thenReturnFalse() {
        Usuario conductorUserNoProfile = new Usuario(2L, "driveruser", "pass", Rol.CONDUCTOR, null, null, true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUserNoProfile.getUsername(), conductorUserNoProfile.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUserNoProfile.getUsername())).thenReturn(Optional.of(conductorUserNoProfile));

        assertFalse(securityUtils.isConductorIdentificacionLinkedToCurrentUser(conductorProfile.getIdentificacion()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUserNoProfile.getUsername());
    }

    /**
     * Test para verificar isVehiculoIdAssignedToCurrentUser cuando el usuario CONDUCTOR no tiene perfil de conductor.
     */
    @Test
    @DisplayName("isVehiculoIdAssignedToCurrentUser debe retornar false si el usuario CONDUCTOR no tiene perfil de conductor")
    void isVehiculoIdAssignedToCurrentUser_whenConductorUserNoProfile_thenReturnFalse() {
        Usuario conductorUserNoProfile = new Usuario(2L, "driveruser", "pass", Rol.CONDUCTOR, null, null, true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUserNoProfile.getUsername(), conductorUserNoProfile.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUserNoProfile.getUsername())).thenReturn(Optional.of(conductorUserNoProfile));

        assertFalse(securityUtils.isVehiculoIdAssignedToCurrentUser(vehiculoConductor1.getId()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUserNoProfile.getUsername());
        verify(vehiculoRepository, never()).findById(anyLong()); // No debe intentar buscar el vehículo
    }

    /**
     * Test para verificar isVehiculoPlacaAssignedToCurrentUser cuando el usuario CONDUCTOR no tiene perfil de conductor.
     */
    @Test
    @DisplayName("isVehiculoPlacaAssignedToCurrentUser debe retornar false si el usuario CONDUCTOR no tiene perfil de conductor")
    void isVehiculoPlacaAssignedToCurrentUser_whenConductorUserNoProfile_thenReturnFalse() {
        Usuario conductorUserNoProfile = new Usuario(2L, "driveruser", "pass", Rol.CONDUCTOR, null, null, true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(conductorUserNoProfile.getUsername(), conductorUserNoProfile.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(conductorUserNoProfile.getUsername())).thenReturn(Optional.of(conductorUserNoProfile));

        assertFalse(securityUtils.isVehiculoPlacaAssignedToCurrentUser(vehiculoConductor1.getPlaca()));
        verify(usuarioRepository, times(1)).findByUsername(conductorUserNoProfile.getUsername());
        verify(vehiculoRepository, never()).findByPlaca(anyString()); // No debe intentar buscar el vehículo
    }

    /**
     * Test para verificar isClienteOwnedByCurrentUser cuando el usuario CLIENTE no tiene perfil de cliente.
     */
    @Test
    @DisplayName("isClienteOwnedByCurrentUser debe retornar false si el usuario CLIENTE no tiene perfil de cliente")
    void isClienteOwnedByCurrentUser_whenClienteUserNoProfile_thenReturnFalse() {
        Usuario clienteUserNoProfile = new Usuario(3L, "clientuser", "pass", Rol.CLIENTE, null, null, true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new User(clienteUserNoProfile.getUsername(), clienteUserNoProfile.getPassword(), Collections.emptyList()));
        when(usuarioRepository.findByUsername(clienteUserNoProfile.getUsername())).thenReturn(Optional.of(clienteUserNoProfile));

        assertFalse(securityUtils.isClienteOwnedByCurrentUser(clienteProfile.getId()));
        verify(usuarioRepository, times(1)).findByUsername(clienteUserNoProfile.getUsername());
    }
}
