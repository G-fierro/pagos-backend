package com.banduairlines.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "solicitudes_cambio_correo",
        indexes = {
                @Index(
                        name = "idx_cambio_correo_usuario_estado",
                        columnList = "usuario_id, fecha_confirmacion, fecha_cancelacion"
                ),
                @Index(
                        name = "idx_cambio_correo_nuevo_correo",
                        columnList = "nuevo_correo"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudCambioCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nuevo_correo", nullable = false, length = 150)
    private String nuevoCorreo;

    @Column(name = "codigo_hash", length = 100)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    public boolean estaVigente(Instant ahora) {
        return puedeUtilizarse()
                && fechaExpiracion != null
                && fechaExpiracion.isAfter(ahora);
    }

    public boolean estaVencida(Instant ahora) {
        return fechaExpiracion == null || !fechaExpiracion.isAfter(ahora);
    }

    public boolean fueConfirmada() {
        return fechaConfirmacion != null;
    }

    public boolean fueCancelada() {
        return fechaCancelacion != null;
    }

    public boolean puedeUtilizarse() {
        return !fueConfirmada() && !fueCancelada() && codigoHash != null;
    }
}
