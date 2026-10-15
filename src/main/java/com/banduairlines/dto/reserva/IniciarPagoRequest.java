package com.banduairlines.dto.reserva;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Cuerpo de POST /reservas/{reservaId}/pago: el total en CLP que el pasajero vio en pantalla. */
public record IniciarPagoRequest(
        @NotNull(message = "el totalEsperado es obligatorio")
        @PositiveOrZero(message = "el totalEsperado no puede ser negativo")
        Long totalEsperado
) {
}
