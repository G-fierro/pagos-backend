package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.IniciarPagoRequest;
import com.banduairlines.dto.reserva.PagoIniciadoResponse;

/** RESERVA, endpoint 11: revalidar el precio e iniciar el pago (R-05, R-05b, RF-05, RF-06). */
public interface ReservaPagoService {

    PagoIniciadoResponse iniciarPago(Long reservaId, String compraToken, IniciarPagoRequest request);
}
