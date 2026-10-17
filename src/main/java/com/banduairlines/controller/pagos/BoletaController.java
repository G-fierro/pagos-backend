package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.BoletaResponse;
import com.banduairlines.service.pagos.PagoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/pagos")
public class BoletaController {

    private final PagoService pagoService;

    public BoletaController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/reservas/{reservaId}/boleta")
    public BoletaResponse obtenerBoletaPorReserva(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken
    ) {
        return pagoService.obtenerBoletaPorReserva(reservaId, compraToken);
    }

    @GetMapping("/boletas/{boletaId}")
    public BoletaResponse obtenerBoletaPorId(@PathVariable Long boletaId) {
        return pagoService.obtenerBoletaPorId(boletaId);
    }

}
