package com.banduairlines.dto.reserva;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /reservas/{reservaId}/contacto (R-02, sección Contacto). */
public record ContactoRequest(
        @NotBlank(message = "el nombreCompleto es obligatorio")
        @Size(max = 160, message = "el nombreCompleto no puede superar los 160 caracteres")
        String nombreCompleto,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede superar los 150 caracteres")
        String correo,

        @NotBlank(message = "el telefono es obligatorio")
        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "el telefono debe tener entre 8 y 20 dígitos, con + opcional")
        String telefono
) {
}
