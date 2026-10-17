package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.IniciarPagoRequest;
import com.banduairlines.dto.pagos.PagoIniciadoResponse;
import com.banduairlines.service.pagos.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internas/pagos")
public class PagoInternoController {

    private final PagoService pagoService;

    public PagoInternoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoIniciadoResponse iniciarPago(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody IniciarPagoRequest request
    ) {
        return pagoService.iniciarPago(request, serviceKey);
    }
}
