package com.banduairlines.dto.reserva;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /reservas/{reservaId}/servicios. Reemplaza la lista completa (vacía = ninguno). */
public record ContratarServiciosRequest(
        @NotNull(message = "la lista de servicios es obligatoria (puede ir vacía)")
        List<@Valid @NotNull(message = "el servicio no puede ser nulo") Servicio> servicios
) {

    public record Servicio(
            @NotBlank(message = "el servicioId es obligatorio")
            String servicioId,

            @NotNull(message = "el pasajeroId es obligatorio")
            Long pasajeroId,

            @NotNull(message = "el tramoId es obligatorio porque los servicios se cobran por tramo")
            Long tramoId,

            @NotNull(message = "la cantidad es obligatoria")
            @Min(value = 1, message = "la cantidad debe ser al menos 1")
            Integer cantidad
    ) {
    }
}
