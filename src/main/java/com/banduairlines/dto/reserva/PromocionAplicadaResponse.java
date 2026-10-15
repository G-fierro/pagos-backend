package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;

/** Respuesta de PUT /reservas/{reservaId}/promocion. */
public record PromocionAplicadaResponse(
        Promocion promocion,
        TotalResponse total
) {

    /** valor: porcentaje (0-100) o monto en CLP según tipoDescuento. */
    public record Promocion(
            String codigo,
            String tipoDescuento,
            long valor,
            long descuentoCLP,
            OffsetDateTime vigenteHasta
    ) {
    }
}
