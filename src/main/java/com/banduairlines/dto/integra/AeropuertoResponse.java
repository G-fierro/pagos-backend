package com.banduairlines.dto.integra;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
public record AeropuertoResponse(
        Long id,
        String codigoIata,
        String nombre,
        String ciudad,
        String pais,
        String zonaHoraria
) {
}
