package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PagoIniciadoResponse(
    Long pagoId,
    String codigoReserva,
    EstadoPago estado,
    String urlPago,
    Integer montoCLP,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant expiraEn
) {}
