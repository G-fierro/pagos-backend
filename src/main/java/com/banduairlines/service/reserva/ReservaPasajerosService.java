package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;

/** RESERVA, endpoint 3: datos de todos los pasajeros (R-02, RF-03, RF-05). */
public interface ReservaPasajerosService {

    PasajerosRegistradosResponse registrar(Long reservaId, String compraToken, RegistrarPasajerosRequest request);
}
