package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.DetalleCotizacionResponse;
import com.banduairlines.dto.busqueda.SeleccionCotizacionRequest;
import com.banduairlines.service.busqueda.CotizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
public class CotizacionController {

    // --- Dependencias ---

    private final CotizacionService cotizacionService;

    // --- Endpoints ---

    @PostMapping("/cotizar")
    public ResponseEntity<DetalleCotizacionResponse> cotizarSeleccion(
            @Valid @RequestBody SeleccionCotizacionRequest request) {

        return ResponseEntity.ok(cotizacionService.calcularCotizacion(request));
    }
}