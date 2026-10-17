package com.banduairlines.dto.pagos;

import jakarta.validation.constraints.NotNull;

public record IniciarPagoRequest(
    @NotNull(message = "El reservaId es obligatorio")
    Long reservaId
) {}
