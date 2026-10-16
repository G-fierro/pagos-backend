package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;

/** RESERVA, endpoints 5 y 6: mapa y selección de asientos con bloqueo temporal (R-03, RF-01, RF-05). */
public interface ReservaAsientosService {

    MapaAsientosResponse obtenerMapa(Long reservaId, Long tramoId, String compraToken);

    AsientosAsignadosResponse seleccionar(Long reservaId, String compraToken, SeleccionarAsientosRequest request);
}
