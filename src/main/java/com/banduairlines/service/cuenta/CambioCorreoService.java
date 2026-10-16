package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.ConfirmacionCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaSolicitudCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudCambioCorreo;

public interface CambioCorreoService {

    RespuestaSolicitudCambioCorreo solicitar(SolicitudCambioCorreo solicitud);

    RespuestaUsuario confirmar(Long idSolicitud, ConfirmacionCambioCorreo confirmacion);

    void cancelar(Long idSolicitud);
}
