package com.banduairlines.dto.pagos;

public record TotalMedioPagoResponse(
        String medioPago,
        Integer cantidadPagos,
        Integer montoTotalCLP
) {
}
