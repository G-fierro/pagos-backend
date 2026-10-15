package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.AplicarPromocionRequest;
import com.banduairlines.dto.reserva.PromocionAplicadaResponse;
import com.banduairlines.dto.reserva.TotalReservaResponse;
import com.banduairlines.service.reserva.ReservaPromocionService;

import jakarta.validation.Valid;

/** RESERVA, endpoints 9 y 10 (R-05). Acceso: JWT del dueño o X-Compra-Token del invitado. */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}/promocion")
public class ReservaPromocionController {

    private final ReservaPromocionService reservaPromocionService;

    public ReservaPromocionController(ReservaPromocionService reservaPromocionService) {
        this.reservaPromocionService = reservaPromocionService;
    }

    @PutMapping
    public PromocionAplicadaResponse aplicarPromocion(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody AplicarPromocionRequest request) {
        return reservaPromocionService.aplicar(reservaId, compraToken, request);
    }

    @DeleteMapping
    public TotalReservaResponse quitarPromocion(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
        return reservaPromocionService.quitar(reservaId, compraToken);
    }
}
