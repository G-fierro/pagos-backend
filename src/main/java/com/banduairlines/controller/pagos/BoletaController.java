package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.BoletaPdfDto;
import com.banduairlines.dto.pagos.BoletaResponse;
import com.banduairlines.dto.pagos.BoletaResumenResponse;
import com.banduairlines.dto.pagos.ConsultaBoletaInvitadoRequest;
import com.banduairlines.dto.pagos.PaginaResponse;
import com.banduairlines.service.pagos.PagoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/pagos")
public class BoletaController {

    private final PagoService pagoService;

    public BoletaController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/reservas/{reservaId}/boleta")
    public BoletaResponse obtenerBoletaPorReserva(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken
    ) {
        return pagoService.obtenerBoletaPorReserva(reservaId, compraToken);
    }

    @GetMapping("/boletas/historial")
    public PaginaResponse<BoletaResumenResponse> obtenerHistorial(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamano", defaultValue = "10") int tamano,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "codigoReserva", required = false) String codigoReserva
    ) {
        return pagoService.obtenerHistorialBoletas(pagina, tamano, desde, hasta, codigoReserva);
    }

    @GetMapping("/boletas/{boletaId}")
    public BoletaResponse obtenerBoletaPorId(@PathVariable Long boletaId) {
        return pagoService.obtenerBoletaPorId(boletaId);
    }

    @GetMapping("/boletas/{boletaId}/pdf")
    public ResponseEntity<byte[]> descargarBoletaPdf(
            @PathVariable Long boletaId,
            @RequestHeader(value = "X-Compra-Token", required = false) String compraToken
    ) {
        BoletaPdfDto pdf = pagoService.generarBoletaPdf(boletaId, compraToken);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"boleta-" + pdf.numeroBoleta() + ".pdf\"")
                .body(pdf.contenido());
    }

    @PostMapping("/boletas/consulta")
    public BoletaResponse consultarBoletaInvitado(@Valid @RequestBody ConsultaBoletaInvitadoRequest request) {
        return pagoService.consultarBoletaInvitado(request);
    }
}
