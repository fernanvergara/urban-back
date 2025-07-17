package com.transporte.urbanback.model;

import com.transporte.urbanback.enums.EstadoPedido; // Necesitarás crear este ENUM
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedidos")
@Schema(description = "Representa un pedido de transporte urbano, conectando clientes, vehículos y conductores")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del pedido", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id") 
    @Schema(description = "Cliente asignado a este pedido")
    private Cliente cliente;

    @NotBlank(message = "La dirección de origen no puede estar vacía")
    @Schema(description = "Dirección de origen del pedido", example = "Calle 10 # 5-20, Centro")
    private String direccionOrigen;

    @NotBlank(message = "La dirección de destino no puede estar vacía")
    @Schema(description = "Dirección de destino del pedido", example = "Carrera 8 # 15-30, Sur")
    private String direccionDestino;

    @NotNull(message = "La fecha y hora de creación del pedido no puede ser nula")
    @Column(nullable = false)
    @Schema(description = "Fecha y hora en que se creó el pedido", example = "2023-11-01T09:00:00")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha y hora estimada de recogida del pedido", example = "2023-11-01T09:30:00")
    private LocalDateTime fechaRecogidaEstimada;

    @Schema(description = "Fecha y hora real de recogida del pedido", example = "2023-11-01T09:35:00")
    private LocalDateTime fechaRecogidaReal;

    @Schema(description = "Fecha y hora estimada de entrega del pedido", example = "2023-11-01T10:30:00")
    private LocalDateTime fechaEntregaEstimada;

    @Schema(description = "Fecha y hora real de entrega del pedido", example = "2023-11-01T10:25:00")
    private LocalDateTime fechaEntregaReal;

    @NotNull(message = "El estado del pedido no puede ser nulo")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "Estado actual del pedido", example = "PENDIENTE", allowableValues = {"PENDIENTE", "ASIGNADO", "EN_CAMINO", "COMPLETADO", "CANCELADO"})
    private EstadoPedido estado; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id") 
    @Schema(description = "Vehículo asignado a este pedido")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id") 
    @Schema(description = "Conductor asignado a este pedido")
    private Conductor conductor;

    @DecimalMin(value = "0.01", message = "El peso del pedido debe ser mayor a cero")
    @Column(precision = 10, scale = 3)
    @Schema(description = "Peso del pedido en kilogramos", example = "150.75")
    private BigDecimal pesoKg;

    @Schema(description = "Notas adicionales sobre el pedido", example = "Requiere manejo especial")
    private String notas;

}