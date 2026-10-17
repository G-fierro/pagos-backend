package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PagoBoletaDto(
    EstadoPago estado,
    MedioPagoDto medioPago,
    Long ordenFlow,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fechaPago
) {}
