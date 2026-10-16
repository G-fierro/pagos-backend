package com.banduairlines.dto.pagos;

public record TotalMedioPagoDto(
    String medioPago,
    Integer cantidadPagos,
    Long montoTotalCLP
) {}
