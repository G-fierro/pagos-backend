package com.banduairlines.dto.reserva;

/** Asiento de un pasajero en un tramo. asiento es null si continúa sin asiento; precioCLP es lo cobrado por él. */
public record AsignacionAsiento(Long pasajeroId, Long tramoId, String asiento, long precioCLP) {
}
