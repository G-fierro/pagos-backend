package com.banduairlines.service.impl;

import org.springframework.stereotype.Service;

import com.banduairlines.model.Reserva;
import com.banduairlines.service.reserva.CobroReservaService;

/**
 * PROVISIONAL hasta que PAGOS entregue su integración con Flow: no registra el Pago (eso es de PAGOS)
 * ni devuelve URL de pasarela, solo permite probar el flujo de RESERVA de punta a punta.
 * PAGOS borra esta clase al publicar su propia implementación de CobroReservaService.
 */
@Service
public class CobroReservaServiceProvisional implements CobroReservaService {

    @Override
    public CobroIniciado iniciarCobro(Reserva reserva, long montoCLP, String correo) {
        return new CobroIniciado("PROVISIONAL-" + reserva.getId(), null);
    }
}
