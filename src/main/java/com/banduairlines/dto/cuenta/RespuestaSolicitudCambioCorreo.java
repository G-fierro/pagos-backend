package com.banduairlines.dto.cuenta;

import java.time.Instant;

public record RespuestaSolicitudCambioCorreo(
        Long idSolicitud,
        String correoEnmascarado,
        Instant fechaExpiracion
) {
}
