package com.banduairlines.service.admin;

import org.springframework.data.domain.Page;

import com.banduairlines.dto.admin.RutaRespuesta;
import com.banduairlines.dto.admin.RutaSolicitud;
import java.util.List;

public interface RutaService {
    Page<RutaRespuesta> listar(
            Boolean activa,
            int pagina,
            int tamaño);

    Page<RutaRespuesta> buscar(
            Boolean activa,
            String texto,
            int pagina,
            int tamano);

    RutaRespuesta obtener(Long id);
    RutaRespuesta crear(RutaSolicitud solicitud);
    RutaRespuesta actualizar(Long id, RutaSolicitud solicitud);
    RutaRespuesta cambiarEstado(Long id, boolean activa);
    List<RutaRespuesta> listarTodas(Boolean activa);
    List<RutaRespuesta> buscarTodas(Boolean activa, String texto);

}
