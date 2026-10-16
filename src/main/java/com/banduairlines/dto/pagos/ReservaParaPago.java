package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoReserva;

import java.time.Instant;
import java.util.List;

public record ReservaParaPago(
    Long reservaId,
    String codigoReserva,
    EstadoReserva estado,
    Instant expiraEn,
    ContactoDto contacto,
    List<PasajeroCompraDto> pasajeros,
    TotalReservaDto total
) {}
