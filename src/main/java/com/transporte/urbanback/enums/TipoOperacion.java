package com.transporte.urbanback.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipos de operaciones de auditoría")
public enum TipoOperacion {
    CREAR,      // Operación de creación de un registro
    ACTUALIZAR, // Operación de actualización de un registro
    CAMBIO_ESTADO,    // Operación de eliminación lógica de un registro, o reactivación
    ELIMINAR    // Operación de eliminación física de un registro
}
