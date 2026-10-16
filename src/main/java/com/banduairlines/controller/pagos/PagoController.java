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

    @GetMapping("/{pagoId}")
    public PagoDetalleResponse obtenerPago(
            @PathVariable Long pagoId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken
    ) {
        return pagoService.obtenerPago(pagoId, compraToken);
    }

    @PostMapping(value = "/notificaciones", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void recibirNotificacionFlow(@RequestParam(value = "token", required = false) String token) {
        pagoService.procesarNotificacionFlow(token);
    }

    @PostMapping(value = "/retorno", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> retornoFlow(@RequestParam(value = "token", required = false) String token) {
        String destino = pagoService.procesarRetornoFlow(token);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, destino)
                .build();
    }

    @PostMapping(value = "/reembolsos/notificaciones", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void recibirNotificacionReembolso(@RequestParam(value = "token", required = false) String token) {
        pagoService.procesarNotificacionReembolso(token);
    }
}
