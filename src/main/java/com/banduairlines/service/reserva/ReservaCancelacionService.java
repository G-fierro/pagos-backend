package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaCanceladaResponse;

/** RESERVA, endpoint 12: abandonar la compra, y la cancelación automática al vencer expiraEn. */
public interface ReservaCancelacionService {

    ReservaCanceladaResponse cancelar(Long reservaId, String compraToken);

    /** Cancela las compras abiertas cuyo bloqueo venció. Devuelve cuántas canceló. */
    int cancelarExpiradas();
}
