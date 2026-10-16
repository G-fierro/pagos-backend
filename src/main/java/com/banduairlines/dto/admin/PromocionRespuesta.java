package com.banduairlines.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.enums.TipoDescuento;

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
