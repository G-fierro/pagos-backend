package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;

import com.banduairlines.model.enums.EstadoReserva;

/**
 * Respuesta de POST /reservas (201). compraToken solo viene en compras de invitado:
 * el cliente lo envía en X-Compra-Token en las siguientes operaciones.
 */
public record ReservaCreadaResponse(
        Long reservaId,
        EstadoReserva estado,
        String compraToken,
        OffsetDateTime expiraEn,
        TotalResponse total
) {
}
