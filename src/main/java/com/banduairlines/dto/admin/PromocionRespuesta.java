package com.banduairlines.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.enums.TipoDescuento;

<<<<<<< HEAD
=======
/**
 * Respuesta con los datos de un código promocional (RF-08).
 */
>>>>>>> 42ac198cde31e8f942d25a7888192c17f4dcdb7a
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
