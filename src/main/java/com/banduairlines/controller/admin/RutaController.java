package com.banduairlines.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.banduairlines.service.admin.RutaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.dto.admin.EstadoRutaSolicitud;
import com.banduairlines.dto.admin.RutaRespuesta;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/rutas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RutaController {
    private final RutaService servicio;

    @GetMapping
    public Page<RutaRespuesta> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano, @RequestParam(required = false) Boolean activa) {
        return servicio.listar(activa, pagina, tamano);
    }

    @GetMapping("/{id}")
    public RutaRespuesta obtener(@PathVariable @Positive Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<RutaRespuesta> crear(@Valid @RequestBody RutaSolicitud solicitud) {
        RutaRespuesta respuesta = servicio.crear(solicitud);
        URI ubicacion = URI.create("/api/v1/admin/rutas/" + respuesta.id());
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @PutMapping("/{id}")
    public RutaRespuesta actualizar(@PathVariable @Positive Long id, @Valid @RequestBody RutaSolicitud solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public RutaRespuesta cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EstadoRutaSolicitud solicitud) {

        return servicio.cambiarEstado(id, solicitud.activa());
    }

}
