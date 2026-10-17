package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowPaymentCreateResponse(
    String url,
    String token,
    Long flowOrder
) {}
