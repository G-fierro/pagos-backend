package com.banduairlines.dto.reserva;

import java.util.List;

/** Respuesta de PUT /reservas/{reservaId}/pasajeros: la lista con el pasajeroId de cada uno. */
public record PasajerosRegistradosResponse(List<PasajeroResponse> pasajeros) {
}
