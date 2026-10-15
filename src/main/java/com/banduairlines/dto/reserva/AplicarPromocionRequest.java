package com.banduairlines.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /reservas/{reservaId}/promocion. */
public record AplicarPromocionRequest(
        @NotBlank(message = "el código promocional es obligatorio")
        @Size(max = 30, message = "el código promocional no puede superar los 30 caracteres")
        String codigo
) {
}
