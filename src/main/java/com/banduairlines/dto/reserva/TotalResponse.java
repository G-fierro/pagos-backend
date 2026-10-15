package com.banduairlines.dto.reserva;

import java.util.List;

/**
 * Desglose del total que muestran "Resumen lateral" y "Detalle del total".
 * concepto: TARIFA, ASIENTOS, EQUIPAJE, SERVICIOS o DESCUENTO (este último con monto negativo).
 */
public record TotalResponse(
        List<Linea> lineas,
        long descuentoCLP,
        long totalCLP,
        String moneda
) {

    public record Linea(String concepto, String descripcion, long montoCLP) {
    }
}
