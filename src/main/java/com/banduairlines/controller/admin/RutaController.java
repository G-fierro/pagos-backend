package com.banduairlines.controller.admin;

import com.banduairlines.model.Ruta;
import com.banduairlines.service.RutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    @GetMapping
    public ResponseEntity<List<Ruta>> listarRutas() {
        return ResponseEntity.ok(rutaService.listarRutas());
    }

    @PostMapping
    public ResponseEntity<Ruta> crearRuta(
            @RequestParam Long origenId,
            @RequestParam Long destinoId,
            @RequestParam Integer distanciaKm,
            @RequestParam Integer duracionMin) {

        Ruta nuevaRuta = rutaService.crearRuta(origenId, destinoId, distanciaKm, duracionMin);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Ruta> cambiarEstadoRuta(
            @PathVariable Long id,
            @RequestParam Boolean activa) {
        Ruta rutaActualizada = rutaService.cambiarEstadoRuta(id, activa);
        return ResponseEntity.ok(rutaActualizada);
    }
}
