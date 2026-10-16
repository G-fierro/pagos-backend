package com.banduairlines.service.cuenta;

import java.time.Instant;

public interface ServicioCorreo {

    void enviarCodigoCambioCorreo(
            String destinatario,
            String codigo,
            Instant fechaExpiracion
    );
}
