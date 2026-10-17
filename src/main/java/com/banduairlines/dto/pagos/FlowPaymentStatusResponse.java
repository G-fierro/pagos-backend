package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowPaymentStatusResponse(
    Long flowOrder,
    String commerceOrder,
    Integer status,
    Integer amount,
    String currency,
    String payer,
    PaymentData paymentData
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentData(
        String date,
        String media,
        CardData card
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardData(
        String type,
        String brand,
        String last4CardDigits
    ) {}
}
