package com.banduairlines.dto.reserva;

import java.util.List;

import com.banduairlines.model.enums.ClaseCabina;

/**
 * Respuesta de GET /reservas/{reservaId}/tramos/{tramoId}/asientos (R-03).
 * estado: DISPONIBLE, SELECCIONADO (tomado por esta reserva) o NO_DISPONIBLE.
 * categoria: ESTANDAR o PREFERENTE. precioCLP es lo que cuesta elegirlo con el pack de la reserva.
 */
public record MapaAsientosResponse(
        Long tramoId,
        ClaseCabina cabina,
        List<String> columnas,
        List<String> pasillosDespuesDe,
        List<Fila> filas
) {

    public record Fila(int numero, ClaseCabina cabina, List<Asiento> asientos) {
    }

    public record Asiento(
            String codigo,
            String columna,
            String categoria,
            String estado,
            long precioCLP,
            boolean incluidoEnPack
    ) {
    }
}
