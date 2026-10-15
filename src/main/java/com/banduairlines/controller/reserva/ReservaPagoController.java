package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.IniciarPagoRequest;
import com.banduairlines.dto.reserva.PagoIniciadoResponse;
import com.banduairlines.dto.reserva.ReservaCanceladaResponse;
import jakarta.validation.Valid;
import com.banduairlines.service.reserva.ReservaCancelacionService;
import com.banduairlines.service.reserva.ReservaPagoService;

/** RESERVA, endpoints 11 (R-05, R-05b) y 12 (abandonar la compra). */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}")
public class ReservaPagoController {

    private final ReservaPagoService reservaPagoService;
    private final ReservaCancelacionService reservaCancelacionService;

    public ReservaPagoController(ReservaPagoService reservaPagoService,
                                 ReservaCancelacionService reservaCancelacionService) {
        this.reservaPagoService = reservaPagoService;
        this.reservaCancelacionService = reservaCancelacionService;
    }

    @PostMapping("/pago")
    public PagoIniciadoResponse iniciarPago(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody IniciarPagoRequest request) {
        return reservaPagoService.iniciarPago(reservaId, compraToken, request);
    }

    @PostMapping("/cancelacion")
    public ReservaCanceladaResponse abandonarCompra(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
        return reservaCancelacionService.cancelar(reservaId, compraToken);
    }
}
