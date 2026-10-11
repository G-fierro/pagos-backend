package com.banduairlines.service;

import com.banduairlines.dto.integra.AeropuertoRequest;
import com.banduairlines.dto.integra.AeropuertoResponse;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
public interface AeropuertoService {

    AeropuertoResponse obtenerPorId(Long id);

    AeropuertoResponse crear(AeropuertoRequest request);
}
