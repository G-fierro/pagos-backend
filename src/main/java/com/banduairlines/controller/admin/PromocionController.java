package com.banduairlines.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.admin.EstadoSolicitud;
import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;
import com.banduairlines.service.admin.PromocionService;

import jakarta.validation.Valid;

/**
 * Endpoints del módulo Administración para configurar los códigos promocionales
 * (RF-08). Solo accesible para el rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/v1/admin/promociones")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PromocionRespuesta> listar(@RequestParam(required = false) Boolean activa) {
        return promocionService.listar(activa);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionRespuesta obtener(@PathVariable Long id) {
        return promocionService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionRespuesta crear(@Valid @RequestBody PromocionSolicitud solicitud) {
        return promocionService.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody PromocionSolicitud solicitud) {
        return promocionService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PromocionRespuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoSolicitud solicitud) {
        return promocionService.cambiarEstado(id, solicitud.activo());
    }
}
