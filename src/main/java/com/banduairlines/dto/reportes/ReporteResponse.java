package com.banduairlines.dto.reportes;

public record ReporteResponse(
        int id_reporte,
        int total_venta,
        int total_ingreso,
        int asiento_ocupado,
        int cantidad_reservas,
        int cantidad_adultos,
        int cantidad_ninios,
        int cantidad_bebes,
        int anio,
        String mes,
        int dia
) {
}