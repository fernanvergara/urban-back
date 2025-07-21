package com.transporte.urbanback.service.impl;

import com.transporte.urbanback.dto.RegisterRequest;
import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.model.Conductor;
import com.transporte.urbanback.repository.ConductorRepository;
import com.transporte.urbanback.repository.UsuarioRepository;
import com.transporte.urbanback.security.Usuario;
import com.transporte.urbanback.service.UsuarioService;
import com.transporte.urbanback.exception.ResourceNotFoundException; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, ConductorRepository conductorRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    @Transactional
    public Usuario registrarNuevoUsuario(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario '" + request.getUsername() + "' ya esta en uso.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsername(request.getUsername());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRol(request.getRol());
        nuevoUsuario.setActivo(true);

        if (request.getRol() == Rol.CONDUCTOR) {
            if (request.getConductorId() == null) {
                throw new IllegalArgumentException("Para el rol CONDUCTOR, es obligatorio vincular un 'conductorId' existente.");
            }
            Conductor conductor = conductorRepository.findById(request.getConductorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor con ID " + request.getConductorId() + " no encontrado para vincular.")); // CAMBIADO

            nuevoUsuario.setConductor(conductor);
        }

        return usuarioRepository.save(nuevoUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorNombreUsuario(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public Usuario crearYGuardarAdmin(String username, String rawPassword) {
        return usuarioRepository.findByUsername(username).orElseGet(() -> {
            Usuario adminUser = new Usuario();
            adminUser.setUsername(username);
            adminUser.setPassword(passwordEncoder.encode(rawPassword));
            adminUser.setRol(Rol.ADMIN);
            adminUser.setActivo(true);
            return usuarioRepository.save(adminUser);
        });
    }

    @Override
    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario cambiarEstadoUsuario(Long id, boolean nuevoEstado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id)); 

        if (usuario.getActivo() != nuevoEstado) {
            int updatedRows = usuarioRepository.updateActivoStatus(id, nuevoEstado);
            if (updatedRows > 0) {
                usuario.setActivo(nuevoEstado); 
                return usuario;
            } else {
                throw new IllegalStateException("No se pudo actualizar el estado del usuario con ID: " + id); 
            }
        }
        return usuario; 
    }

    @Override
    public List<Rol> obtenerTodosLosRoles() {
        return Arrays.asList(Rol.values());
    }
}
