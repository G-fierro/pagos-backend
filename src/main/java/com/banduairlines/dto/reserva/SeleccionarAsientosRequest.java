package com.banduairlines.dto.reserva;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PUT /reservas/{reservaId}/asientos (R-03). Solo se envían los pasajeros cuyo asiento cambia;
 * asiento null libera el asiento (continuar sin asiento).
 */
public record SeleccionarAsientosRequest(
        @NotEmpty(message = "debe indicar al menos una selección")
        List<@Valid @NotNull(message = "la selección no puede ser nula") Seleccion> selecciones
) {

    public record Seleccion(
            @NotNull(message = "el pasajeroId es obligatorio")
            Long pasajeroId,

            @NotNull(message = "el tramoId es obligatorio")
            Long tramoId,

            @Pattern(regexp = "^[0-9]{1,2}[A-Za-z]$", message = "el asiento debe tener el formato fila y letra, por ejemplo 12A")
            String asiento
    ) {
    }

    @AssertTrue(message = "un pasajero no puede aparecer dos veces en el mismo tramo")
    public boolean isSeleccionesSinRepetir() {
        if (selecciones == null) {
            return true;
        }
        List<String> claves = selecciones.stream().filter(Objects::nonNull)
                .map(s -> s.pasajeroId() + ":" + s.tramoId()).toList();
        return claves.size() == claves.stream().distinct().count();
    }
}
