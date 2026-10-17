package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ResultadoPagoRequest(
    Long pagoId,
    Long transaccionId,
    EstadoPago estado,
    Integer montoCLP,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fecha,
    String motivoRechazo
) {}
