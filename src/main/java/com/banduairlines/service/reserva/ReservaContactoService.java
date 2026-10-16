package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;

/** RESERVA, endpoint 4: datos de contacto de la compra (R-02, RF-03). */
public interface ReservaContactoService {

    ContactoResponse registrar(Long reservaId, String compraToken, ContactoRequest request);
}
