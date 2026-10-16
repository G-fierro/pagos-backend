package com.banduairlines.dto.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceptorDto(
    String nombreCompleto,
    String rut
) {}
