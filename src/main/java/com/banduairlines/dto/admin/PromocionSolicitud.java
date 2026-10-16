package com.banduairlines.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.enums.TipoDescuento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

<<<<<<< HEAD
=======
/**
 * Solicitud para crear o actualizar un código promocional (RF-08).
 */
>>>>>>> 42ac198cde31e8f942d25a7888192c17f4dcdb7a
public record PromocionSolicitud(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        String codigo,

        @NotNull(message = "El tipo de descuento es obligatorio")
        TipoDescuento tipoDescuento,

        @NotNull(message = "El valor del descuento es obligatorio")
        @Positive(message = "El valor del descuento debe ser mayor que cero")
        BigDecimal valor,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime vigenteDesde,

        @NotNull(message = "La fecha de expiración es obligatoria")
        LocalDateTime vigenteHasta,

        @Positive(message = "Los usos máximos deben ser mayores que cero")
        Integer usosMaximos
) {
}
