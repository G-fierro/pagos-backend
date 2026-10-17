package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

public record BoletaResponse(
    Long boletaId,
    Long numeroBoleta,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fechaEmision,
    String formaEmision,
    Long reservaId,
    String codigoReserva,
    String receptor,
    List<String> pasajeros,
    List<LineaBoletaDto> lineas,
    Integer descuentoCLP,
    Integer totalCLP,
    PagoBoletaDto pago
) {}
