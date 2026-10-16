package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

public record EstadoRutaSolicitud(

        @NotNull(message = "El estado de activación es obligatorio") Boolean activa

) {
}