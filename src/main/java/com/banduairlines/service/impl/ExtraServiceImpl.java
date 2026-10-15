package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.admin.ExtraRespuesta;
import com.banduairlines.dto.admin.ExtraSolicitud;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Extra;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.service.admin.ExtraService;

/**
 * Implementación del servicio de gestión de servicios adicionales (RF-07).
 */
@Service
@Transactional
public class ExtraServiceImpl implements ExtraService {

    private final ExtraRepository extraRepository;

    public ExtraServiceImpl(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraRespuesta> listar(Boolean activo) {
        return extraRepository.listarAdministracion(activo)
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExtraRespuesta obtener(Long id) {
        return mapearADto(exigirExtra(id));
    }

    @Override
    public ExtraRespuesta crear(ExtraSolicitud solicitud) {
        Extra extra = Extra.builder()
                .tipo(solicitud.tipo())
                .nombre(solicitud.nombre())
                .pesoKg(solicitud.pesoKg())
                .precio(solicitud.precio())
                .activo(true)
                .build();

        return mapearADto(extraRepository.save(extra));
    }

    @Override
    public ExtraRespuesta actualizar(Long id, ExtraSolicitud solicitud) {
        Extra extra = exigirExtra(id);

        extra.setTipo(solicitud.tipo());
        extra.setNombre(solicitud.nombre());
        extra.setPesoKg(solicitud.pesoKg());
        extra.setPrecio(solicitud.precio());

        return mapearADto(extra);
    }

    @Override
    public ExtraRespuesta cambiarEstado(Long id, boolean activo) {
        Extra extra = exigirExtra(id);
        extra.setActivo(activo);
        return mapearADto(extra);
    }

    private Extra exigirExtra(Long id) {
        return extraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Extra", id));
    }

    private ExtraRespuesta mapearADto(Extra extra) {
        return new ExtraRespuesta(
                extra.getId(),
                extra.getTipo(),
                extra.getNombre(),
                extra.getPesoKg(),
                extra.getPrecio(),
                extra.getActivo()
        );
    }
}
