package com.banduairlines.controller.tracking;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloResponse;
import com.banduairlines.service.tracking.TrackingAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tracking")
public class TrackingAdminController {

    private final TrackingAdminService trackingAdminService;

    public TrackingAdminController(TrackingAdminService trackingAdminService) {
        this.trackingAdminService = trackingAdminService;
    }

    @PostMapping("/vuelos/{vueloId}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SeguimientoVueloResponse> actualizarEstado(
            @PathVariable Long vueloId,
            @Valid @RequestBody ActualizarEstadoRequest request) {

        SeguimientoVueloResponse response = trackingAdminService.actualizarEstadoVuelo(vueloId, request);
        return ResponseEntity.ok(response);
    }
}