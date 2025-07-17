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
@Table(name = "vehiculos_audit")
@Schema(description = "Registro de auditoría de cambios en la entidad Vehiculo")
public class VehiculoAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del registro de auditoría", example = "1")
    private Long id;

    @NotNull // El vehículo auditado no debe ser nulo
    @ManyToOne(fetch = FetchType.LAZY) // Relación Muchos a Uno con Vehiculo
    @JoinColumn(name = "vehiculo_id", nullable = false) // Columna en esta tabla que referencia al ID de Vehiculo
    @Schema(description = "Vehículo afectado por el cambio", example = "{\"id\": 101, \"placa\": \"ABC-123\"}")
    private Vehiculo vehiculo; 

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "Tipo de operación realizada (CREAR, ACTUALIZAR, ELIMINAR)", example = "ACTUALIZAR", allowableValues = {"CREAR", "ACTUALIZAR", "ELIMINAR"})
    private TipoOperacion tipoOperacion;

    @NotNull
    @Column(nullable = false)
    @Schema(description = "Fecha y hora en que se realizó el cambio", example = "2023-10-26T10:30:00")
    private LocalDateTime fechaCambio;

    @NotNull // El usuario que realiza el cambio no debe ser nulo
    @ManyToOne(fetch = FetchType.LAZY) // Relación Muchos a Uno con Usuario
    @JoinColumn(name = "usuario_id", nullable = false) // Columna en esta tabla que referencia al ID de Usuario
    @Schema(description = "Usuario que realizó el cambio", example = "{\"id\": 1, \"username\": \"admin_user\"}")
    private Usuario usuarioEditor; 

    @Column(columnDefinition = "TEXT") // Para almacenar los valores antiguos y nuevos como JSON o String
    @Schema(description = "Detalles del cambio en formato JSON (valores antiguos y nuevos)", example = "{\"old_value\": \"Marca A\", \"new_value\": \"Marca B\"}")
    private String detallesCambio; // Podría ser un JSON string con los cambios (ej. {"campo": "valor_antiguo", "campo": "valor_nuevo"})

    /**
     * Constructor para facilitar la creación de registros de auditoría.
     * @param vehiculo El vehiculo sobre el cual se realizó la operación.
     * @param tipoOperacion El tipo de operación (CREAR, ACTUALIZAR, ELIMINAR).
     * @param usuarioEditor El usuario que realizó la operación.
     * @param detallesCambio Una cadena JSON con los detalles del cambio.
     */
    public VehiculoAudit(Vehiculo vehiculo, TipoOperacion tipoOperacion, Usuario usuarioEditor, String detallesCambio) {
        this.vehiculo = vehiculo;
        this.tipoOperacion = tipoOperacion;
        this.fechaCambio = LocalDateTime.now();
        this.usuarioEditor = usuarioEditor;
        this.detallesCambio = detallesCambio;
    }
}