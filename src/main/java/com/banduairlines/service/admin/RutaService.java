package com.banduairlines.service.admin;

import org.springframework.data.domain.Page;

import com.banduairlines.dto.admin.RutaRespuesta;
import com.banduairlines.dto.admin.RutaSolicitud;


public interface RutaService {
    Page<RutaRespuesta> listar(
        Boolean activa,
        int pagina,
        int tamaño
    );

    RutaRespuesta obtener(Long id);
    RutaRespuesta crear (RutaSolicitud solicitud);
    RutaRespuesta actualizar(Long id, RutaSolicitud solicitud);
    RutaRespuesta cambiarEstado(Long id, boolean activa);

}
