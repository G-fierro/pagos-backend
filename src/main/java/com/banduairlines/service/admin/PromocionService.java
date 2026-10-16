package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;

public interface PromocionService {

    List<PromocionRespuesta> listar(Boolean activa);

    PromocionRespuesta obtener(Long id);

    PromocionRespuesta crear(PromocionSolicitud solicitud);

    PromocionRespuesta actualizar(Long id, PromocionSolicitud solicitud);

    PromocionRespuesta cambiarEstado(Long id, boolean activa);
}
