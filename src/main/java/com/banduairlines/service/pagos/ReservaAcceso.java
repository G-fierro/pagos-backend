package com.banduairlines.service.pagos;

import com.banduairlines.model.Reserva;

public interface ReservaAcceso {

    Reserva obtenerAutorizada(Long reservaId, String compraToken);
}
