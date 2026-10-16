package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.SolicitudCambioContrasena;

public interface CambioContrasenaService {

    void cambiar(SolicitudCambioContrasena solicitud);
}
