package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.*;

import java.time.LocalDate;

public interface PagoService {

    PagoIniciadoResponse iniciarPago(IniciarPagoRequest request, String serviceKey);

    PagoDetalleResponse obtenerPago(Long pagoId, String compraToken);

    void procesarNotificacionFlow(String token);

    String procesarRetornoFlow(String token);

    void procesarNotificacionReembolso(String token);

    BoletaResponse obtenerBoletaPorReserva(Long reservaId, String compraToken);





    boolean tienePagoEnEspera(Long reservaId);
}
