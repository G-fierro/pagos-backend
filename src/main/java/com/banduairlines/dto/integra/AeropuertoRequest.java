package com.banduairlines.dto.integra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
public record AeropuertoRequest(
        @NotBlank(message = "el código IATA es obligatorio")
        @Size(min = 3, max = 3, message = "el código IATA debe tener exactamente 3 caracteres")
        @Pattern(regexp = "^[A-Z]{3}$", message = "el código IATA debe contener 3 letras mayúsculas")
        String codigoIata,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar los 120 caracteres")
        String nombre,

        @NotBlank(message = "la ciudad es obligatoria")
        @Size(max = 80, message = "la ciudad no puede superar los 80 caracteres")
        String ciudad,

        @Size(max = 60, message = "el país no puede superar los 60 caracteres")
        String pais,

        @Size(max = 40, message = "la zona horaria no puede superar los 40 caracteres")
        String zonaHoraria
) {
}
