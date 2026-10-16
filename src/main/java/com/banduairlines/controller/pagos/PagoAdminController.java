package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.PaginaResponse;
import com.banduairlines.dto.pagos.PagoRegistroResponse;
import com.banduairlines.dto.pagos.ResumenPagosResponse;
import com.banduairlines.service.pagos.PagoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/pagos")
public class PagoAdminController {

    private final PagoService pagoService;

    public PagoAdminController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResumenPagosResponse obtenerResumen(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return pagoService.obtenerResumenReportes(desde, hasta);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PaginaResponse<PagoRegistroResponse> listarPagos(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamano", defaultValue = "10") int tamano
    ) {
        return pagoService.obtenerPagosReportes(desde, hasta, pagina, tamano);
    }
}
