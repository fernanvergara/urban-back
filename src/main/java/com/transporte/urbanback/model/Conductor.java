package com.transporte.urbanback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.transporte.urbanback.constants.Constantes;

@Setter
@Getter
@NoArgsConstructor 
@AllArgsConstructor 
@Entity
@Table(name = "conductores")
@Schema(description = "Detalles de un conductor en el sistema de transporte urbano")
public class Conductor {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Schema(description = "Identificador único del conductor", example = "1")
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Column(nullable = false, length = 100)
    @Schema(description = "Nombre completo del conductor", example = "Juan Pérez")
    private String nombreCompleto;

    @NotBlank(message = "La identificación no puede estar vacía")
    @Column(unique = true, nullable = false, length = 20)
    @Schema(description = "Número de identificación único del conductor (Cédula, licencia, etc.)", example = "1012345678")
    private String identificacion;

    @NotNull(message = "La fecha de nacimiento no puede ser nula")
    @JsonFormat(pattern = Constantes.FORMATO_FECHA_DDMMYYYY) // Formato para entrada y salida JSON (ej. 15/05/1985)
    @Schema(description = "Fecha de nacimiento del conductor (formato DD/MM/YYYY)", example = "15/05/1985")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El número de teléfono no puede estar vacío")
    @Column(nullable = false, length = 20)
    @Pattern(regexp = Constantes.REGEX_TELEFONO_COLOMBIA, message = "Formato de teléfono no válido. Debe ser +57 seguido de 10 dígitos.") 
    @Schema(description = "Número de teléfono del conductor", example = "+573001234567")
    private String telefono;

    @Column(nullable = false)
    @Schema(description = "Indica si el conductor está activo o dado de baja lógicamente", example = "true")
    private Boolean activo = true; // Campo para eliminación lógica

}