package com.banduairlines.dto.reportes;

import jakarta.validation.constraints.*;

public record ReporteRequest(

        @NotNull(message = "El total de ventas es obligatorio")
        @PositiveOrZero(message = "El total de ventas no puede ser negativo")
        Integer total_venta,

        @NotNull(message = "El total de ingresos es obligatorio")
        @PositiveOrZero(message = "El total de ingresos no puede ser negativo")
        Integer total_ingreso,

        @NotNull(message = "La cantidad de asientos ocupados es obligatoria")
        @PositiveOrZero(message = "La cantidad de asientos ocupados no puede ser negativa")
        Integer asiento_ocupado,

        @NotNull(message = "La cantidad de reservas es obligatoria")
        @PositiveOrZero(message = "La cantidad de reservas no puede ser negativa")
        Integer cantidad_reservas,

        @NotNull(message = "La cantidad de adultos es obligatoria")
        @PositiveOrZero(message = "La cantidad de adultos no puede ser negativa")
        Integer cantidad_adultos,

        @NotNull(message = "La cantidad de niños es obligatoria")
        @PositiveOrZero(message = "La cantidad de niños no puede ser negativa")
        Integer cantidad_ninios,

        @NotNull(message = "La cantidad de bebés es obligatoria")
        @PositiveOrZero(message = "La cantidad de bebés no puede ser negativa")
        Integer cantidad_bebes,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 1, message = "El año debe ser mayor que cero")
        Integer anio,

        @NotBlank(message = "El mes es obligatorio")
        @Pattern(
                regexp = "^(ENERO|FEBRERO|MARZO|ABRIL|MAYO|JUNIO|JULIO|AGOSTO|SEPTIEMBRE|OCTUBRE|NOVIEMBRE|DICIEMBRE)$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El mes debe ser un nombre válido, como ENERO o FEBRERO"
        )
        String mes,

        @NotNull(message = "El día es obligatorio")
        @Min(value = 1, message = "El día debe ser como mínimo 1")
        @Max(value = 31, message = "El día no puede superar 31")
        Integer dia
) {
}