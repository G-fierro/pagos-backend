package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudRegistro;

public interface RegistroPasajeroService {

    RespuestaUsuario registrar(SolicitudRegistro solicitud);
}
