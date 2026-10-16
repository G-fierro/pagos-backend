package com.banduairlines.dto.busqueda;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetalleCotizacionResponse {

    // --- Identificadores de Selección ---

    private Long vueloId;

    private Long tarifaId;

    private String claseCabina;

    // --- Desglose de Precios ---

    private BigDecimal subtotalAdultos;

    private BigDecimal subtotalNinos;

    private BigDecimal subtotalBebes;

    private BigDecimal impuestos;

    private BigDecimal precioTotal;
}