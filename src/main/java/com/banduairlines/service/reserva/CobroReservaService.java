package com.banduairlines.service.reserva;

import com.banduairlines.model.Reserva;

/**
 * Lo que RESERVA necesita de PAGOS para iniciar el cobro (contrato de RESERVA, sección 6:
 * "POST /pagos con { reservaId, montoCLP, correo } → { pagoId, urlPago }").
 * PAGOS debe entregar la implementación real; mientras tanto existe CobroReservaServiceProvisional.
 * Si PAGOS o Flow fallan, la implementación lanza ServicioExternoException.
 */
public interface CobroReservaService {

    CobroIniciado iniciarCobro(Reserva reserva, long montoCLP, String correo);

    record CobroIniciado(String pagoId, String urlPago) {
    }
}
