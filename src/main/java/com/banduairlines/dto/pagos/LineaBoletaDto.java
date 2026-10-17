package com.banduairlines.dto.pagos;

public record LineaBoletaDto(
    String concepto,
    String descripcion,
    Integer montoCLP
) {}
