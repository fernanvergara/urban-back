package com.transporte.urbanback.security; // En el mismo paquete que Usuario

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import com.transporte.urbanback.enums.TipoOperacion; // Importa el enum de operaciones

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios_audit")
@Schema(description = "Registro de auditoría de cambios en la entidad Usuario")
public class UsuarioAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del registro de auditoría", example = "1")
    private Long id;

    @NotNull // El usuario auditado (el que sufrió el cambio) no debe ser nulo
    @ManyToOne(fetch = FetchType.LAZY) // Relación Muchos a Uno con Usuario (el usuario auditado)
    @JoinColumn(name = "usuario_auditado_id", nullable = false) // Columna en esta tabla que referencia al ID del Usuario que fue modificado
    @Schema(description = "Usuario afectado por el cambio", example = "{\"id\": 1, \"username\": \"admin\"}")
    private Usuario usuarioAuditado; // El usuario sobre el que se realizó la operación

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "Tipo de operación realizada (CREAR, ACTUALIZAR, ELIMINAR)", example = "ACTUALIZAR", allowableValues = {"CREAR", "ACTUALIZAR", "ELIMINAR"})
    private TipoOperacion tipoOperacion;

    @NotNull
    @Column(nullable = false)
    @Schema(description = "Fecha y hora en que se realizó el cambio", example = "2023-10-26T11:00:00")
    private LocalDateTime fechaCambio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY) // Relación Muchos a Uno con Usuario (el que realizó el cambio)
    @JoinColumn(name = "usuario_que_cambio_id", nullable = false) // Columna en esta tabla que referencia al ID del Usuario que realizó el cambio
    @Schema(description = "Usuario que realizó el cambio", example = "{\"id\": 1, \"username\": \"admin\"}")
    private Usuario usuarioEditor; // El usuario logueado que realizó la operación

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Estado completo del registro *después* de la operación, en formato JSON. Vacío si es eliminación.", example = "{\"id\": 1, \"username\": \"nuevo_admin\", \"rol\": \"ADMIN\"}")
    private String detallesCambio;

    // Constructor para facilitar la creación de registros de auditoría
    public UsuarioAudit(Usuario usuarioAuditado, TipoOperacion tipoOperacion, Usuario usuarioEditor, String detallesCambio) {
        this.usuarioAuditado = usuarioAuditado;
        this.tipoOperacion = tipoOperacion;
        this.fechaCambio = LocalDateTime.now();
        this.usuarioEditor = usuarioEditor;
        this.detallesCambio = detallesCambio;
    }
}