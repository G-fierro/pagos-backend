package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para activar o desactivar un servicio adicional (RF-07).
 */
public record EstadoSolicitud(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}
