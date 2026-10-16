package com.banduairlines.controller.admin;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

import com.banduairlines.dto.admin.EstadoRutaSolicitud;
import com.banduairlines.dto.admin.RutaRespuesta;
import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.service.admin.RutaService;

@RestController
@RequestMapping("/api/v1/admin/rutas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RutaController {

    private final RutaService rutaService;

    @GetMapping("/{id}")
    public RutaRespuesta obtener(@PathVariable @Positive Long id) {
        return rutaService.obtener(id);
    }

    @GetMapping
    public Page<RutaRespuesta> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) String buscar) {

        if (buscar == null || buscar.isBlank()) {
            return rutaService.listar(activa, pagina, tamano);
        }

        return rutaService.buscar(activa, buscar, pagina, tamano);
    }

    @GetMapping("/todas")
    public List<RutaRespuesta> listarTodas(
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) String buscar) {

        if (buscar == null || buscar.isBlank()) {
            return rutaService.listarTodas(activa);
        }

        return rutaService.buscarTodas(activa, buscar);
    }

    @PostMapping
    public ResponseEntity<RutaRespuesta> crear(
            @Valid @RequestBody RutaSolicitud solicitud) {

        RutaRespuesta respuesta = rutaService.crear(solicitud);
        URI ubicacion = URI.create("/api/v1/admin/rutas/" + respuesta.id());

        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @PutMapping("/{id}")
    public RutaRespuesta actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RutaSolicitud solicitud) {

        return rutaService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public RutaRespuesta cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EstadoRutaSolicitud solicitud) {

        return rutaService.cambiarEstado(id, solicitud.activa());
    }
}