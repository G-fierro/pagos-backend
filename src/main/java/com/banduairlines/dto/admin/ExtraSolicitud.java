package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import com.banduairlines.model.enums.TipoExtra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear o actualizar un servicio adicional (RF-07).
 */
public record ExtraSolicitud(
        @NotNull(message = "El tipo de servicio es obligatorio")
        TipoExtra tipo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @Positive(message = "El peso debe ser mayor que cero")
        Integer pesoKg,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        BigDecimal precio
) {
}
