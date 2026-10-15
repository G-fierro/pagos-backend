package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.CatalogoServiciosResponse;
import com.banduairlines.dto.reserva.ContratarServiciosRequest;
import com.banduairlines.dto.reserva.ServiciosContratadosResponse;

/** RESERVA, endpoints 7 y 8: equipaje y servicios adicionales (R-04, RF-04). */
public interface ReservaServiciosService {

    CatalogoServiciosResponse obtenerCatalogo(Long reservaId, String compraToken);

    ServiciosContratadosResponse contratar(Long reservaId, String compraToken, ContratarServiciosRequest request);
}
