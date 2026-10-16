package com.banduairlines.controller.busqueda;

import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;
import com.banduairlines.service.busqueda.FiltroBusquedaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
public class FiltroBusquedaController {

    // --- Dependencias ---

    private final FiltroBusquedaService filtroBusquedaService;

    // --- Endpoints ---

    @GetMapping("/vuelos")
    public ResponseEntity<?> obtenerVuelosFiltrados(@Valid @ModelAttribute FiltroBusquedaRequest request) {
        return ResponseEntity.ok(filtroBusquedaService.filtrarYOrdenar(request));
    }
}