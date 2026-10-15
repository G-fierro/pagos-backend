package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.AplicarPromocionRequest;
import com.banduairlines.dto.reserva.PromocionAplicadaResponse;
import com.banduairlines.dto.reserva.TotalReservaResponse;

/** RESERVA, endpoints 9 y 10: código promocional (R-05, RF-05). */
public interface ReservaPromocionService {

    PromocionAplicadaResponse aplicar(Long reservaId, String compraToken, AplicarPromocionRequest request);

    TotalReservaResponse quitar(Long reservaId, String compraToken);
}
