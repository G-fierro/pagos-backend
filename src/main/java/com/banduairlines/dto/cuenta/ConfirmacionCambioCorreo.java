package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmacionCambioCorreo(
        @NotBlank(message = "El codigo es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "El codigo debe contener exactamente seis digitos")
        String codigo
) {
}
