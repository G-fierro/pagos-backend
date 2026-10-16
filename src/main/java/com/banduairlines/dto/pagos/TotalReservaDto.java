package com.banduairlines.dto.pagos;

import java.util.List;

public record TotalReservaDto(
    List<LineaCompraDto> lineas,
    Integer descuentoCLP,
    Integer totalCLP
) {}
