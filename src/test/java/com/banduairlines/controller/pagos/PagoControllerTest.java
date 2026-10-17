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
                        @RequestHeader(value = "X-Compra-Token", required = false) String compraToken) {
                return pagoService.obtenerPago(pagoId, compraToken);
        }
}