package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record BoletaResumenResponse(
    Long boletaId,
    Long numeroBoleta,
    String codigoReserva,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fechaEmision,
    Integer totalCLP
) {}
