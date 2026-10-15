package com.banduairlines.dto.reserva;

import java.util.List;

/** Respuesta de GET /reservas/{reservaId}/servicios (R-04). */
public record CatalogoServiciosResponse(
        List<Incluido> incluidos,
        List<Disponible> disponibles
) {

    /** tipo: ARTICULO_PERSONAL, EQUIPAJE_CABINA o EQUIPAJE_BODEGA. */
    public record Incluido(String tipo, int cantidad, String descripcion) {
    }

    /** categoria: EQUIPAJE, EMBARQUE_PRIORITARIO u OTRO. */
    public record Disponible(
            String servicioId,
            String nombre,
            String categoria,
            long precioCLP,
            boolean porTramo,
            int maximoPorPasajero
    ) {
    }
}
