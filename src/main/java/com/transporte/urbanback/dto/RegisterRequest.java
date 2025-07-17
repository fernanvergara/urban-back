package com.transporte.urbanback.dto;

import com.transporte.urbanback.enums.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Objeto de solicitud para el registro de un nuevo usuario")
public class RegisterRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Schema(description = "Nombre de usuario único para el registro", example = "nuevo.usuario")
    private String username;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Schema(description = "Contraseña para el nuevo usuario", example = "PasswordSegura!1")
    private String password;

    @NotNull(message = "El rol no puede ser nulo")
    @Schema(description = "Rol del nuevo usuario", example = "CLIENTE", allowableValues = {"ADMIN", "CONDUCTOR", "CLIENTE"})
    private Rol rol;

    // Campo opcional para vincular un conductor existente si el rol es CONDUCTOR
    @Schema(description = "ID del conductor existente a vincular (solo si el rol es CONDUCTOR)", example = "10", nullable = true)
    private Long conductorId;
    
    // Campo opcional para vincular un conductor existente si el rol es CLIENTE
    @Schema(description = "ID del cliente existente a vincular (solo si el rol es CLIENTE)", example = "11", nullable = true)
    private Long clienteId; 
}