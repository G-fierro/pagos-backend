package com.banduairlines.dto.pagos;

public record EmisorDto(
    String razonSocial,
    String rut,
    String giro,
    String direccion,
    String telefono
) {}
