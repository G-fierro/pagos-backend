package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowRefundResponse(
    String type,
    Long flowRefundOrder,
    String date,
    String status,
    Integer amount,
    String token
) {}
