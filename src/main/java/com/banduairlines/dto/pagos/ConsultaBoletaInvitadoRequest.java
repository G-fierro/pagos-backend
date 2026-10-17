package com.banduairlines.dto.pagos;

import jakarta.validation.constraints.NotBlank;

public record ConsultaBoletaInvitadoRequest(
    @NotBlank(message = "El código de reserva es obligatorio")
    String codigoReserva,
    @NotBlank(message = "El correo es obligatorio")
    String correo
) {}
