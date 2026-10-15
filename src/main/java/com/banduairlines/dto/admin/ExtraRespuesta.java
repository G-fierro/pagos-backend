package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import com.banduairlines.model.enums.TipoExtra;

/**
 * Respuesta con los datos de un servicio adicional (RF-07).
 */
public record ExtraRespuesta(
        Long id,
        TipoExtra tipo,
        String nombre,
        Integer pesoKg,
        BigDecimal precio,
        Boolean activo
) {
}
