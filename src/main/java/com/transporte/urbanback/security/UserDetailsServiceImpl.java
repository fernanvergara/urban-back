package com.transporte.urbanback.security;

import com.transporte.urbanback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Construimos el objeto UserDetails que Spring Security espera
        // Usamos Collections.singletonList para un solo rol, si tuvieras múltiples roles o permisos,
        // necesitarías mapearlos desde la entidad Usuario a una colección de GrantedAuthority.
        return new User(
                usuario.getUsername(),
                usuario.getPassword(), // Spring Security esperará que esta contraseña esté encriptada
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())) // Roles con prefijo "ROLE_"
        );
    }
}