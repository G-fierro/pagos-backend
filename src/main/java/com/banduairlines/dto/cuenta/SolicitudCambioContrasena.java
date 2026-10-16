package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudCambioContrasena(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 32, message = "La nueva contraseña debe tener entre 8 y 32 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{N}.-]+", message = "La nueva contraseña solo puede contener letras, numeros, puntos y guiones")
        String nuevaContrasena,

        @NotBlank(message = "La confirmacion de contraseña es obligatoria")
        @Size(min = 8, max = 32, message = "La confirmacion debe tener entre 8 y 32 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{N}.-]+", message = "La confirmacion solo puede contener letras, numeros, puntos y guiones")
        String confirmacionContrasena
) {
}
