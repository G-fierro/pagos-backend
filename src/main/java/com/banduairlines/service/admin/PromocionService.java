package com.banduairlines.service.admin;

import java.util.List;

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;

<<<<<<< HEAD
=======
/**
 * Servicio del módulo Administración para gestionar los códigos promocionales (RF-08).
 */
>>>>>>> 42ac198cde31e8f942d25a7888192c17f4dcdb7a
public interface PromocionService {

    List<PromocionRespuesta> listar(Boolean activa);

    PromocionRespuesta obtener(Long id);

    PromocionRespuesta crear(PromocionSolicitud solicitud);

    PromocionRespuesta actualizar(Long id, PromocionSolicitud solicitud);

    PromocionRespuesta cambiarEstado(Long id, boolean activa);
}
