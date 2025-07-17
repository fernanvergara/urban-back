package com.transporte.urbanback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import com.transporte.urbanback.enums.TipoOperacion;
import com.transporte.urbanback.security.Usuario;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedidos_audit")
@Schema(description = "Registro de auditoría de cambios en la entidad Pedido")
public class PedidoAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del registro de auditoría", example = "1")
    private Long id;

    @NotNull 
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "pedido_id", nullable = false) 
    @Schema(description = "Pedido afectado por el cambio", example = "{\"id\": 101, \"estado\": \"PENDIENTE\"}")
    private Pedido pedido; 

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "Tipo de operación realizada (CREAR, ACTUALIZAR, ELIMINAR)", example = "CREAR", allowableValues = {"CREAR", "ACTUALIZAR", "ELIMINAR"})
    private TipoOperacion tipoOperacion;

    @NotNull
    @Column(nullable = false)
    @Schema(description = "Fecha y hora en que se realizó el cambio", example = "2023-10-26T10:45:00")
    private LocalDateTime fechaCambio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @Schema(description = "Usuario que realizó el cambio", example = "{\"id\": 1, \"username\": \"admin_user\"}")
    private Usuario usuarioEditor;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Estado completo del registro *después* de la operación, en formato JSON. Vacío si es eliminación.", example = "{\"id\": 101, \"estado\": \"EN_PROGRESO\", \"clienteId\": 1, \"conductorId\": 5, ...}")
    private String detallesCambio;

    /**
     * Constructor para facilitar la creación de registros de auditoría de Pedido.
     * @param pedido El pedido sobre el cual se realizó la operación.
     * @param tipoOperacion El tipo de operación (CREAR, ACTUALIZAR, ELIMINAR).
     * @param usuarioEditor El usuario que realizó la operación.
     * @param detallesCambio Una cadena JSON con los detalles del cambio.
     */
    public PedidoAudit(Pedido pedido, TipoOperacion tipoOperacion, Usuario usuarioEditor, String detallesCambio) {
        this.pedido = pedido;
        this.tipoOperacion = tipoOperacion;
        this.fechaCambio = LocalDateTime.now();
        this.usuarioEditor = usuarioEditor;
        this.detallesCambio = detallesCambio;
    }
}