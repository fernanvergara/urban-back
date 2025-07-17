package com.transporte.urbanback.enums;

public enum EstadoPedido {
    PENDIENTE,    // El pedido ha sido creado y está esperando ser asignado
    ASIGNADO,     // El pedido ha sido asignado a un vehículo y conductor
    EN_CAMINO,    // El conductor ha iniciado el viaje con el pedido
    COMPLETADO,   // El pedido ha sido entregado exitosamente
    CANCELADO     // El pedido ha sido cancelado
}
