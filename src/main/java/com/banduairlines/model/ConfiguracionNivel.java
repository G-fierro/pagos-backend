package com.banduairlines.model;

import java.math.BigDecimal;

public record ConfiguracionNivel(
        String nivel,
        long millasMinimas,
        BigDecimal multiplicadorPuntos
) {
}
