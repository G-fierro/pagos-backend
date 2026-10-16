package com.banduairlines.dto.reserva;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Sentido;

/**
 * Respuesta de GET /reservas/{reservaId} (R-05, R-07): itinerario, pasajeros, asientos, servicios,
 * promoción, contacto, total y estado. codigoReserva es null hasta que se inicia el pago.
 */
public record ReservaResponse(
        Long reservaId,
        String codigoReserva,
        EstadoReserva estado,
        OffsetDateTime expiraEn,
        String pack,
        CantidadPasajeros cantidadPasajeros,
        List<Tramo> itinerario,
        List<PasajeroResponse> pasajeros,
        List<AsignacionAsiento> asientos,
        List<ServiciosContratadosResponse.Servicio> servicios,
        PromocionAplicadaResponse.Promocion promocion,
        ContactoResponse contacto,
        TotalResponse total
) {

    public record CantidadPasajeros(Integer adultos, Integer ninos, Integer bebes) {
    }

    /** salida y llegada en hora local del aeropuerto, como las guarda ADMIN. */
    public record Tramo(
            Long tramoId,
            int orden,
            Sentido sentido,
            String codigoVuelo,
            String origen,
            String destino,
            LocalDateTime salidaProgramada,
            LocalDateTime llegadaProgramada,
            ClaseCabina cabina,
            EstadoVuelo estadoVuelo
    ) {
    }
}
