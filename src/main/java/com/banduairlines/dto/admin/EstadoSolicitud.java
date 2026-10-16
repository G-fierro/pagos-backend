package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

<<<<<<< HEAD
=======
/**
 * Solicitud para activar o desactivar un recurso del módulo de administración.
 */
>>>>>>> 42ac198cde31e8f942d25a7888192c17f4dcdb7a
public record EstadoSolicitud(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}
