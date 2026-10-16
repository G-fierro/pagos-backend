package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para activar o desactivar un recurso del módulo de administración.
 */
public record EstadoSolicitud(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}
