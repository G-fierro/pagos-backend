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

    PaginaResponse<BoletaResumenResponse> obtenerHistorialBoletas(int pagina, int tamano);

    BoletaResponse obtenerBoletaPorId(Long boletaId);

    byte[] generarBoletaPdf(Long boletaId, String compraToken);

    BoletaResponse consultarBoletaInvitado(ConsultaBoletaInvitadoRequest request);

    ResumenPagosResponse obtenerResumenReportes(LocalDate desde, LocalDate hasta);

    PaginaResponse<PagoRegistroResponse> obtenerPagosReportes(LocalDate desde, LocalDate hasta, int pagina, int tamano);

    boolean tienePagoEnEspera(Long reservaId);

    ResumenPagosResponse obtenerResumenAdmin(LocalDate desde, LocalDate hasta);

    PaginaResponse<PagoRegistroResponse> listarPagosAdmin(int pagina, int tamano);
}
