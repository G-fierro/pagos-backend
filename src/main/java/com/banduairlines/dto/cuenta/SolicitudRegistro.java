package com.banduairlines.dto.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRegistro(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 32, message = "La contraseña debe tener entre 8 y 32 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{N}.-]+", message = "La contraseña solo puede contener letras, numeros, puntos y guiones")
        String contrasena,

        @NotBlank(message = "La confirmacion de contraseña es obligatoria")
        @Size(min = 8, max = 32, message = "La confirmacion debe tener entre 8 y 32 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{N}.-]+", message = "La confirmacion solo puede contener letras, numeros, puntos y guiones")
        String confirmacionContrasena,

        @Null(message = "El rol no puede ser enviado por el cliente")
        @Schema(hidden = true)
        Object rol,

        @Null(message = "El nivel no puede ser enviado por el cliente")
        @Schema(hidden = true)
        Object nivel
) {
    public SolicitudRegistro {
        correo = correo == null ? null : correo.trim();
    }

    public SolicitudRegistro(
            String nombres,
            String apellidos,
            String correo,
            String contrasena,
            String confirmacionContrasena
    ) {
        this(
                nombres,
                apellidos,
                correo,
                contrasena,
                confirmacionContrasena,
                null,
                null
        );
    }
}
