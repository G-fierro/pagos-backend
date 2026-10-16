package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

public record EstadoSolicitud(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}
