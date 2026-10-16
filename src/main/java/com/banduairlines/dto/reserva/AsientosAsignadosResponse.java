package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;
import java.util.List;

/** Respuesta de PUT /reservas/{reservaId}/asientos: todas las asignaciones, el fin del bloqueo y el total. */
public record AsientosAsignadosResponse(
        List<AsignacionAsiento> asignaciones,
        OffsetDateTime expiraEn,
        TotalResponse total
) {
}
