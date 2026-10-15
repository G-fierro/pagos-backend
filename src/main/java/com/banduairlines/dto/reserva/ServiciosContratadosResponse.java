package com.banduairlines.dto.reserva;

import java.util.List;

/** Respuesta de PUT /reservas/{reservaId}/servicios. */
public record ServiciosContratadosResponse(
        List<Servicio> servicios,
        TotalResponse total
) {

    public record Servicio(
            String servicioId,
            Long pasajeroId,
            Long tramoId,
            int cantidad,
            String nombre,
            long subtotalCLP
    ) {
    }
}
