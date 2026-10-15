package com.banduairlines.controller.admin;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.AsientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/asientos")
@RequiredArgsConstructor
public class AsientoController {

    private final AsientoService asientoService;

    @GetMapping("/aeronave/{aeronaveId}")
    public ResponseEntity<List<Asiento>> listarPorAeronave(
            @PathVariable Long aeronaveId,
            @RequestParam(required = false) ClaseCabina claseCabina) {
        if (claseCabina != null) {
            return ResponseEntity.ok(asientoService.obtenerAsientosPorClase(aeronaveId, claseCabina));
        }
        return ResponseEntity.ok(asientoService.obtenerAsientosPorAeronave(aeronaveId));
    }

    @PostMapping
    public ResponseEntity<Asiento> crearAsiento(
            @RequestParam Long aeronaveId,
            @RequestParam Integer fila,
            @RequestParam String letra,
            @RequestParam ClaseCabina claseCabina,
            @RequestParam Boolean esPreferente) {

        Asiento nuevoAsiento = asientoService.crearAsiento(aeronaveId, fila, letra, claseCabina, esPreferente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoAsiento);
    }
}
