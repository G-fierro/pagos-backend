package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record ResumenPagosResponse(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate desde,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate hasta,

        Integer cantidadPagos,
        Integer montoTotalCLP,
        Integer ticketPromedioCLP,
        Integer cantidadRechazados,
        List<TotalMedioPagoResponse> porMedioPago
) {}
