package com.banduairlines.controller.integra;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.integra.AeropuertoRequest;
import com.banduairlines.dto.integra.AeropuertoResponse;
import com.banduairlines.service.AeropuertoService;

import jakarta.validation.Valid;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
@RestController
@RequestMapping("/api/v1/ejemplo/aeropuertos")
public class AeropuertoController {

    private final AeropuertoService aeropuertoService;

    public AeropuertoController(AeropuertoService aeropuertoService) {
        this.aeropuertoService = aeropuertoService;
    }

    @GetMapping("/{id}")
    public AeropuertoResponse obtenerPorId(@PathVariable Long id) {
        return aeropuertoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AeropuertoResponse crear(@Valid @RequestBody AeropuertoRequest request) {
        return aeropuertoService.crear(request);
    }
}
