package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.service.reserva.ReservaContactoService;
import com.banduairlines.service.reserva.ReservaPasajerosService;

import jakarta.validation.Valid;

/** RESERVA, endpoints 3 y 4 (R-02). Acceso: JWT del dueño o X-Compra-Token del invitado. */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}")
public class ReservaPasajerosController {

    private final ReservaPasajerosService reservaPasajerosService;
    private final ReservaContactoService reservaContactoService;

    public ReservaPasajerosController(ReservaPasajerosService reservaPasajerosService,
                                      ReservaContactoService reservaContactoService) {
        this.reservaPasajerosService = reservaPasajerosService;
        this.reservaContactoService = reservaContactoService;
    }

    @PutMapping("/pasajeros")
    public PasajerosRegistradosResponse registrarPasajeros(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody RegistrarPasajerosRequest request) {
        return reservaPasajerosService.registrar(reservaId, compraToken, request);
    }

    @PutMapping("/contacto")
    public ContactoResponse registrarContacto(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody ContactoRequest request) {
        return reservaContactoService.registrar(reservaId, compraToken, request);
    }
}
