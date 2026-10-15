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
import com.banduairlines.dto.admin.ExtraRespuesta;
import com.banduairlines.dto.admin.ExtraSolicitud;
import com.banduairlines.service.admin.ExtraService;

import jakarta.validation.Valid;

/**
 * Endpoints del módulo Administración para configurar los servicios adicionales
 * (RF-07). Solo accesible para el rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/v1/admin/extras")
public class ExtraController {

    private final ExtraService extraService;

    public ExtraController(ExtraService extraService) {
        this.extraService = extraService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ExtraRespuesta> listar(@RequestParam(required = false) Boolean activo) {
        return extraService.listar(activo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ExtraRespuesta obtener(@PathVariable Long id) {
        return extraService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ExtraRespuesta crear(@Valid @RequestBody ExtraSolicitud solicitud) {
        return extraService.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ExtraRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ExtraSolicitud solicitud) {
        return extraService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ExtraRespuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoSolicitud solicitud) {
        return extraService.cambiarEstado(id, solicitud.activo());
    }
}
