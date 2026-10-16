package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.ReservaParaPago;

public interface ReservaConsultaInterna {

    ReservaParaPago obtenerParaPago(Long reservaId);
}
