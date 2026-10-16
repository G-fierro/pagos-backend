package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCambioCorreo(
        @NotBlank(message = "El nuevo correo es obligatorio")
        @Email(message = "El nuevo correo debe tener un formato valido")
        @Size(max = 150, message = "El nuevo correo no puede superar 150 caracteres")
        String nuevoCorreo
) {
    public SolicitudCambioCorreo {
        nuevoCorreo = nuevoCorreo == null ? null : nuevoCorreo.trim();
    }
}
