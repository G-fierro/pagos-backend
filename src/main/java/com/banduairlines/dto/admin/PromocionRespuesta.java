package com.banduairlines.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.enums.TipoDescuento;

/**
 * Respuesta con los datos de un código promocional (RF-08).
 */
public record PromocionRespuesta(
        Long id,
        String codigo,
        TipoDescuento tipoDescuento,
        BigDecimal valor,
        LocalDateTime vigenteDesde,
        LocalDateTime vigenteHasta,
        Integer usosMaximos,
        Integer usosActuales,
        Boolean activa
) {
}
