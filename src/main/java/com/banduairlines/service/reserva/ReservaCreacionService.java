package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;

/** RESERVA, endpoint 1: iniciar la compra con el vuelo y la tarifa elegidos en BÚSQUEDA (R-01). */
public interface ReservaCreacionService {

    ReservaCreadaResponse crear(CrearReservaRequest request);
}
