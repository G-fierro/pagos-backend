package com.banduairlines.model;

import com.banduairlines.model.enums.Nivel;

import java.math.BigDecimal;

public record ConfiguracionNivel(
        Nivel nivel,
        long millasMinimas,
        BigDecimal multiplicadorPuntos
) {
}
