package com.transporte.urbanback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Entity 
@Table(name = "vehiculos") 
@Schema(description = "Detalles de un vehículo en el sistema de transporte urbano")
public class Vehiculo {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Schema(description = "Identificador único del vehículo", example = "1")
    private Long id;

    @NotBlank(message = "La placa no puede estar vacía") // Valida que la placa no esté en blanco
    // Expresión regular para validar el formato de placa "ABC-123" o similar
    // Permite 3 letras seguidas de un guion y 3 números. Adaptable para futuros formatos.
    @Pattern(regexp = "^[A-Z]{3}-\\d{3}$", message = "El formato de la placa debe ser XXX-NNN (ej. ABC-123)") 
    @Column(unique = true, nullable = false, length = 7) // Columna única, no nula y con longitud máxima
    @Schema(description = "Número de placa del vehículo (formato XXX-NNN)", example = "ABC-123", pattern = "^[A-Z]{3}-\\d{3}$")
    private String placa;

    @NotNull(message = "La capacidad no puede ser nula") // Valida que la capacidad no sea nula
    @Column(nullable = false, precision = 10, scale = 3) // Precisión para números decimales
    @Schema(description = "Capacidad de carga del vehículo en kilogramos", example = "2500.50", type = "number", format = "double")
    private BigDecimal capacidadKg; // Refactorizado a BigDecimal para precisión en kilogramos 

    @NotBlank(message = "La marca no puede estar vacía")
    @Schema(description = "Marca del vehículo", example = "Mercedes-Benz")
    private String marca;

    @NotBlank(message = "El modelo no puede estar vacío")
    @Schema(description = "Modelo del vehículo", example = "Sprinter")
    private String modelo;

    @Schema(description = "Año de fabricación del vehículo", example = "2020")
    private Integer anio;

    @NotNull(message = "El estado activo no puede ser nulo")
    @Column(nullable = false)
    @Schema(description = "Indica si el vehículo está activo o dado de baja lógicamente", example = "true")
    private Boolean activo = true; // Campo para eliminación lógica 

    // Relación ManyToOne: Un vehículo tiene un solo conductor, un conductor puede tener muchos vehículos
    @ManyToOne(fetch = FetchType.LAZY) // Carga perezosa del conductor
    @JoinColumn(name = "conductor_id") // Clave foránea en la tabla 'vehiculos' que apunta al ID del conductor
    @Schema(description = "Conductor actualmente asignado a este vehículo")
    private Conductor conductor;
    
}