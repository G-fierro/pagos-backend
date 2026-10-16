package com.banduairlines.service.tracking;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloResponse;

public interface TrackingAdminService {

    SeguimientoVueloResponse actualizarEstadoVuelo(Long vueloId, ActualizarEstadoRequest request);
}