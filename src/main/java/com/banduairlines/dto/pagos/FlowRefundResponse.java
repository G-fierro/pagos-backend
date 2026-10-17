package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Respuesta de refund/create y refund/getStatus de Flow. Flow entrega los montos como texto
 * decimal (por ejemplo "12990.00"), por eso amount es BigDecimal y no Integer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowRefundResponse(
    String type,
    Long flowRefundOrder,
    String date,
    String status,
    BigDecimal amount,
    String token
) {}
