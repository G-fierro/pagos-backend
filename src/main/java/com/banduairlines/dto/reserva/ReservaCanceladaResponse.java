package com.banduairlines.dto.reserva;

import com.banduairlines.model.enums.EstadoReserva;

/**
 * Respuesta de POST /reservas/{reservaId}/cancelacion.
 * El contrato pide la Reserva completa; se cambia por el DTO de GET /reservas/{reservaId} cuando exista.
 */
public record ReservaCanceladaResponse(
        Long reservaId,
        String codigoReserva,
        EstadoReserva estado
) {
}
