package com.transporte.urbanback.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.transporte.urbanback.enums.Rol;
import com.transporte.urbanback.model.Cliente;
import com.transporte.urbanback.model.Conductor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
@Entity 
@Table(name = "usuarios") 
@Schema(description = "Detalles de un usuario del sistema")
public class Usuario {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Column(unique = true, nullable = false, length = 50)
    @Schema(description = "Nombre de usuario único para iniciar sesión", example = "john.doe")
    private String username;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Schema(description = "Contraseña del usuario (se almacenará de forma segura)", example = "password123")
    private String password; // Se almacenará encriptada

    @Enumerated(EnumType.STRING) // Almacena el nombre del Enum como String en la base de datos
    @NotNull(message = "El rol no puede ser nulo")
    @Column(nullable = false)
    @Schema(description = "Rol del usuario en el sistema", example = "CONDUCTOR", allowableValues = {"ADMIN", "CONDUCTOR", "CLIENTE"})
    private Rol rol;

    // Relación opcional OneToOne con Conductor
    @OneToOne(fetch = FetchType.LAZY) // LAZY para no cargar el Conductor a menos que sea necesario
    @JoinColumn(name = "conductor_id", unique = true, nullable = true) // nullable = true porque no todos los usuarios serán conductores
    @Schema(description = "Referencia al perfil de conductor si el usuario tiene el rol CONDUCTOR")
    private Conductor conductor;

    // Relación opcional OneToOne con Cliente
    @OneToOne(fetch = FetchType.LAZY) // LAZY para no cargar el Cliente a menos que sea necesario
    @JoinColumn(name = "cliente_id", unique = true, nullable = true) // nullable = true porque no todos los usuarios serán clientes
    @Schema(description = "Referencia al perfil de cliente si el usuario tiene el rol CLIENTE")
    private Cliente cliente;

    @Column(nullable = false)
    @Schema(description = "Indica si el usuario está activo o dado de baja lógicamente", example = "true")
    private Boolean activo = true; // Campo para eliminación lógica de usuarios

}