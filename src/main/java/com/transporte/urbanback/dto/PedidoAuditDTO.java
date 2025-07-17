package com.transporte.urbanback.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para el historial de auditoría de un pedido")
public class PedidoAuditDTO {

    @Schema(description = "ID único del registro de auditoría", example = "1")
    private Long id;

    @Schema(description = "ID del pedido al que se refiere el registro de auditoría", example = "101")
    private Long pedidoId;

    @Schema(description = "Tipo de operación de auditoría (CREAR, ACTUALIZAR, ELIMINAR)", example = "ACTUALIZAR", allowableValues = {"CREAR", "ACTUALIZAR", "ELIMINAR"})
    private String tipoOperacion;

    @Schema(description = "Fecha y hora en que se realizó el cambio", example = "2025-07-13T23:05:00")
    private LocalDateTime fechaCambio;

    @Schema(description = "Nombre de usuario de quien realizó el cambio", example = "admin_user")
    private String usuarioEditor;

    @Schema(description = "Detalles específicos del cambio realizado en formato JSON", example = "{\"campo\": \"estado\", \"valorAnterior\": \"PENDIENTE\", \"valorNuevo\": \"EN_PROGRESO\"}")
    private String detallesCambio;
}
