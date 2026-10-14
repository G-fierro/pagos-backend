package com.banduairlines.dto.busqueda;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TramoVueloResponse(
        Long vueloId,
        String codigoVuelo,
        String origenCodigoIata,
        String origenCiudad,
        String origenNombre,
        String destinoCodigoIata,
        String destinoCiudad,
        String destinoNombre,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada,
        long duracionMinutos,
        String aeronaveModelo,
        String estadoVuelo
) {}