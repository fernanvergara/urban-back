package com.transporte.urbanback.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Roles de usuario en el sistema")
public enum Rol {
    ADMIN,      // Rol para administradores del sistema 
    CONDUCTOR,  // Rol para conductores 
    CLIENTE     // Rol para clientes 
}
