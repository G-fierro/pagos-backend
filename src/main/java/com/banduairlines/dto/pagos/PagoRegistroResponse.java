package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonFormat;
<<<<<<< Updated upstream
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoRegistroResponse(
    Long pagoId,
    Long reservaId,
    String codigoReserva,
    EstadoPago estado,
    Integer montoCLP,
    String medioPago,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fechaPago,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant creadoEn
) {}
=======

import java.time.Instant;

public record PagoRegistroResponse(
        Long pagoId,
        Long reservaId,
        String codigoReserva,
        EstadoPago estado,
        Integer montoCLP,
        String medioPago,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant fechaPago,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant creadoEn
) {
}
>>>>>>> Stashed changes
