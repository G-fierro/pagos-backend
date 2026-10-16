package com.banduairlines.dto.reserva;

import java.util.List;
import java.util.Objects;

import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.Sentido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /reservas (R-01): la alternativa que el pasajero eligió en BÚSQUEDA.
 * alternativaId es solo una referencia de BÚSQUEDA; la tarifa se resuelve con tramoId + cabina + pack.
 */
public record CrearReservaRequest(
        @Size(max = 100, message = "el alternativaId no puede superar los 100 caracteres")
        String alternativaId,

        @NotEmpty(message = "debe indicar al menos un tramo")
        List<@Valid @NotNull(message = "el tramo no puede ser nulo") Tramo> tramos,

        @NotBlank(message = "el pack es obligatorio")
        @Size(max = 30, message = "el pack no puede superar los 30 caracteres")
        String pack,

        @NotNull(message = "la cantidadPasajeros es obligatoria")
        @Valid
        CantidadPasajeros cantidadPasajeros
) {

    /** tramoId = id del Vuelo. sentido es opcional (IDA por defecto). */
    public record Tramo(
            @NotNull(message = "el tramoId es obligatorio")
            Long tramoId,

            @NotNull(message = "la cabina es obligatoria")
            ClaseCabina cabina,

            Sentido sentido
    ) {
    }

    public record CantidadPasajeros(
            @NotNull(message = "la cantidad de adultos es obligatoria")
            @Min(value = 0, message = "la cantidad de adultos no puede ser negativa")
            Integer adultos,

            @NotNull(message = "la cantidad de niños es obligatoria")
            @Min(value = 0, message = "la cantidad de niños no puede ser negativa")
            Integer ninos,

            @NotNull(message = "la cantidad de bebés es obligatoria")
            @Min(value = 0, message = "la cantidad de bebés no puede ser negativa")
            Integer bebes
    ) {

        public int total() {
            return valor(adultos) + valor(ninos) + valor(bebes);
        }

        /** Adultos y niños; los bebés no ocupan asiento. */
        public int conAsiento() {
            return valor(adultos) + valor(ninos);
        }

        @AssertTrue(message = "debe haber al menos un pasajero")
        public boolean isAlMenosUnPasajero() {
            return total() > 0;
        }

        private static int valor(Integer n) {
            return n == null ? 0 : n;
        }
    }

    @AssertTrue(message = "un mismo tramo no puede repetirse")
    public boolean isTramosSinRepetir() {
        if (tramos == null) {
            return true;
        }
        List<Long> ids = tramos.stream().filter(Objects::nonNull).map(Tramo::tramoId).filter(Objects::nonNull).toList();
        return ids.size() == ids.stream().distinct().count();
    }
}
