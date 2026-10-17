package com.banduairlines.dto.pagos;

public record BoletaPdfDto(
    Long numeroBoleta,
    byte[] contenido
) {}
