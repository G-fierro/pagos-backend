package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.ExtraRespuesta;
import com.banduairlines.dto.admin.ExtraSolicitud;

/**
 * Servicio del módulo Administración para gestionar los servicios adicionales
 * disponibles y su precio (RF-07).
 */
public interface ExtraService {

    List<ExtraRespuesta> listar(Boolean activo);

    ExtraRespuesta obtener(Long id);

    ExtraRespuesta crear(ExtraSolicitud solicitud);

    ExtraRespuesta actualizar(Long id, ExtraSolicitud solicitud);

    ExtraRespuesta cambiarEstado(Long id, boolean activo);
}
