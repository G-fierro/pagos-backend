package com.banduairlines.dto.reserva;

import java.time.LocalDate;

import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;

/** Pasajero de la reserva. ref solo viene en la respuesta de PUT /pasajeros (es la del cliente). */
public record PasajeroResponse(
        Long pasajeroId,
        String ref,
        TipoPasajero tipo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nacionalidad,
        Long adultoResponsableId
) {
}
