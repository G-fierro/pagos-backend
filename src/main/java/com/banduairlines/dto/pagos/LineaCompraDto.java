package com.banduairlines.dto.pagos;

public record LineaCompraDto(
    String concepto,
    String descripcion,
    Integer montoCLP
) {}
