package com.banduairlines.model.enums;

import java.math.BigDecimal;

public enum Nivel {
    BRONCE(BigDecimal.ONE),
    PLATA(new BigDecimal("1.2")),
    ORO(new BigDecimal("1.5"));

    private final BigDecimal multiplicadorPuntos;

    Nivel(BigDecimal multiplicadorPuntos) {
        this.multiplicadorPuntos = multiplicadorPuntos;
    }

    public String getNombre() {
        return name();
    }

    public BigDecimal getMultiplicadorPuntos() {
        return multiplicadorPuntos;
    }
}
