package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;

/** RESERVA, endpoint 2: consultar la reserva completa (R-05, R-07). */
public interface ReservaConsultaService {

    ReservaResponse obtener(Long reservaId, String compraToken);
}
