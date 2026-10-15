package com.banduairlines.controller.reserva;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.reserva.CatalogoServiciosResponse;
import com.banduairlines.dto.reserva.ContratarServiciosRequest;
import com.banduairlines.dto.reserva.ServiciosContratadosResponse;
import com.banduairlines.service.reserva.ReservaServiciosService;

import jakarta.validation.Valid;

/** RESERVA, endpoints 7 y 8 (R-04). Acceso: JWT del dueño o X-Compra-Token del invitado. */
@RestController
@RequestMapping("/api/v1/reservas/{reservaId}/servicios")
public class ReservaServiciosController {

    private final ReservaServiciosService reservaServiciosService;

    public ReservaServiciosController(ReservaServiciosService reservaServiciosService) {
        this.reservaServiciosService = reservaServiciosService;
    }

    @GetMapping
    public CatalogoServiciosResponse obtenerServicios(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
        return reservaServiciosService.obtenerCatalogo(reservaId, compraToken);
    }

    @PutMapping
    public ServiciosContratadosResponse contratarServicios(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken,
            @Valid @RequestBody ContratarServiciosRequest request) {
        return reservaServiciosService.contratar(reservaId, compraToken, request);
    }
}
