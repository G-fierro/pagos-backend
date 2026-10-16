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

    PaginaResponse<BoletaResumenResponse> obtenerHistorialBoletas(int pagina, int tamano,
                                                                  LocalDate desde, LocalDate hasta,
                                                                  String codigoReserva);

    BoletaResponse obtenerBoletaPorId(Long boletaId);

    BoletaPdfDto generarBoletaPdf(Long boletaId, String compraToken);

    BoletaResponse consultarBoletaInvitado(ConsultaBoletaInvitadoRequest request);

    boolean tienePagoEnEspera(Long reservaId);
}
