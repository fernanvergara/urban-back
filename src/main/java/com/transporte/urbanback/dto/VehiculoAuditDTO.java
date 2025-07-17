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
@Schema(description = "DTO para la representación de un registro de auditoría de Vehículo en la API")
public class VehiculoAuditDTO {

    @Schema(description = "Identificador único del registro de auditoría", example = "1")
    private Long id;

    @Schema(description = "ID del vehículo afectado por el cambio", example = "101")
    private Long vehiculoId; // Solo el ID del vehículo, no el objeto completo

    @Schema(description = "Tipo de operación realizada (CREAR, ACTUALIZAR, CAMBIO_ESTADO, ELIMINAR)", example = "ACTUALIZAR")
    private String tipoOperacion; // Como String para la API

    @Schema(description = "Fecha y hora en que se realizó el cambio", example = "2023-10-26T10:30:00")
    private LocalDateTime fechaCambio;

    @Schema(description = "Nombre de usuario que realizó el cambio", example = "admin_user")
    private String usuarioEditor; // Solo el nombre de usuario

    @Schema(description = "Detalles del cambio en formato JSON (valores antiguos y nuevos)", example = "{\"old_value\": \"activo: true\", \"new_value\": \"activo: false\"}")
    private String detallesCambio; // El String JSON de los detalles
}
