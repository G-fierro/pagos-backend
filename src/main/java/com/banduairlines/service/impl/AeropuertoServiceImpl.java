package com.banduairlines.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.integra.AeropuertoRequest;
import com.banduairlines.dto.integra.AeropuertoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.service.AeropuertoService;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
@Service
@Transactional
public class AeropuertoServiceImpl implements AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository;

    public AeropuertoServiceImpl(AeropuertoRepository aeropuertoRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AeropuertoResponse obtenerPorId(Long id) {
        Aeropuerto aeropuerto = aeropuertoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aeropuerto", id));
        return mapearADto(aeropuerto);
    }

    @Override
    public AeropuertoResponse crear(AeropuertoRequest request) {
        if (aeropuertoRepository.existsByCodigoIata(request.codigoIata())) {
            throw new ConflictoException("Ya existe un aeropuerto con el código IATA: " + request.codigoIata());
        }

        Aeropuerto aeropuerto = Aeropuerto.builder()
                .codigoIata(request.codigoIata())
                .nombre(request.nombre())
                .ciudad(request.ciudad())
                .pais(request.pais() != null && !request.pais().isBlank() ? request.pais() : "Chile")
                .zonaHoraria(request.zonaHoraria() != null && !request.zonaHoraria().isBlank() ? request.zonaHoraria() : "America/Santiago")
                .build();

        Aeropuerto guardado = aeropuertoRepository.save(aeropuerto);
        return mapearADto(guardado);
    }

    private AeropuertoResponse mapearADto(Aeropuerto a) {
        return new AeropuertoResponse(
                a.getId(),
                a.getCodigoIata(),
                a.getNombre(),
                a.getCiudad(),
                a.getPais(),
                a.getZonaHoraria()
        );
    }
}
