package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;
import com.banduairlines.service.reserva.ReservaAsientosService;

import jakarta.validation.Valid;

/** RESERVA, endpoints 5 y 6 (R-03). Acceso: JWT del dueño o X-Compra-Token del invitado. */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}")
public class ReservaAsientosController {

    private final ReservaAsientosService reservaAsientosService;

    public ReservaAsientosController(ReservaAsientosService reservaAsientosService) {
        this.reservaAsientosService = reservaAsientosService;
    }

    @GetMapping("/tramos/{tramoId}/asientos")
    public MapaAsientosResponse obtenerMapaAsientos(
            @PathVariable Long reservaId,
            @PathVariable Long tramoId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
        return reservaAsientosService.obtenerMapa(reservaId, tramoId, compraToken);
    }

    @PutMapping("/asientos")
    public AsientosAsignadosResponse seleccionarAsientos(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody SeleccionarAsientosRequest request) {
        return reservaAsientosService.seleccionar(reservaId, compraToken, request);
    }
}
