package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.PagoDetalleResponse;
import com.banduairlines.service.pagos.PagoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping(value = "/notificaciones", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void recibirNotificacionFlow(@RequestParam(value = "token", required = false) String token) {
        pagoService.procesarNotificacionFlow(token);
    }
}
