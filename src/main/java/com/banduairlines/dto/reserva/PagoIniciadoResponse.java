package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;

import com.banduairlines.model.enums.EstadoReserva;

/** Respuesta de POST /reservas/{reservaId}/pago. El frontend redirige a urlPago. */
public record PagoIniciadoResponse(
        Long reservaId,
        String codigoReserva,
        EstadoReserva estado,
        String pagoId,
        String urlPago,
        long montoCLP,
        OffsetDateTime expiraEn
) {
}
