package com.transporte.urbanback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clientes")
@Schema(description = "Detalles de un cliente en el sistema de transporte urbano")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del cliente", example = "1")
    private Long id;

    @NotBlank(message = "El nombre completo no puede estar vacío")
    @Size(max = 100, message = "El nombre completo no puede exceder los 100 caracteres")
    @Schema(description = "Nombre completo del cliente", example = "Juan Pérez García")
    private String nombreCompleto;

    @NotBlank(message = "La identificación no puede estar vacía")
    @Column(unique = true, nullable = false, length = 20)
    @Size(max = 20, message = "La identificación no puede exceder los 20 caracteres")
    @Schema(description = "Número de identificación del cliente (DNI, cédula, etc.)", example = "1020304050")
    private String identificacion;

    @NotBlank(message = "El número de teléfono no puede estar vacío")
    @Column(nullable = false, length = 15)
    @Size(max = 15, message = "El número de teléfono no puede exceder los 15 caracteres")
    @Schema(description = "Número de teléfono del cliente", example = "+573001234567")
    private String telefono;

    @NotBlank(message = "La dirección de residencia no puede estar vacía")
    @Size(max = 255, message = "La dirección de residencia no puede exceder los 255 caracteres")
    @Schema(description = "Dirección de residencia del cliente", example = "Calle Falsa 123, Barrio Imaginario")
    private String direccionResidencia;

    @NotNull(message = "El estado activo no puede ser nulo")
    @Column(nullable = false)
    @Schema(description = "Indica si el cliente está activo en el sistema o dado de baja lógicamente", example = "true")
    private Boolean activo = true; // Campo para eliminación lógica

}