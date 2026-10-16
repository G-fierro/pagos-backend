package com.banduairlines.dto.reserva;

/** Respuesta de PUT /reservas/{reservaId}/contacto y sección contacto de GET /reservas/{reservaId}. */
public record ContactoResponse(String nombreCompleto, String correo, String telefono) {
}
