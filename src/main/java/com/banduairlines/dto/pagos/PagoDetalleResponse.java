package com.banduairlines.dto.pagos;

import com.banduairlines.model.enums.EstadoPago;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoDetalleResponse(
    Long pagoId,
    Long reservaId,
    String codigoReserva,
    EstadoPago estado,
    Integer montoCLP,
    MedioPagoDto medioPago,
    Long ordenFlow,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant fechaPago,
    String motivoRechazo,
    Long boletaId,
    Boolean puedeReintentar
) {}
