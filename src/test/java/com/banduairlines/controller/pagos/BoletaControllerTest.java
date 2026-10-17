package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.service.pagos.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BoletaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private BoletaController boletaController;

    private BoletaResponse boletaEjemplo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(boletaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        PagoBoletaDto pagoBoleta = new PagoBoletaDto(EstadoPago.PAGADO, new MedioPagoDto("Tarjeta de Crédito Visa", "4521"), 3849571L, Instant.now());
        List<LineaBoletaDto> lineas = List.of(new LineaBoletaDto("TARIFA", "Tarifa Light x 1 adulto", 255320));

        boletaEjemplo = new BoletaResponse(
                1021L, 2458731L, Instant.now(), "VENTA_WEB", 5310L, "K7QX2M",
                "Javiera Fernanda Muñoz Soto", List.of("Javiera Fernanda Muñoz Soto"), lineas, 0, 255320, pagoBoleta
        );
    }

    @Test
    void obtenerBoletaPorReserva_cuandoExiste_retorna200() throws Exception {
        when(pagoService.obtenerBoletaPorReserva(eq(5310L), any())).thenReturn(boletaEjemplo);

        mockMvc.perform(get("/api/v1/pagos/reservas/5310/boleta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boletaId").value(1021L))
                .andExpect(jsonPath("$.numeroBoleta").value(2458731L))
                .andExpect(jsonPath("$.codigoReserva").value("K7QX2M"))
                .andExpect(jsonPath("$.receptor").value("Javiera Fernanda Muñoz Soto"))
                .andExpect(jsonPath("$.emisor").doesNotExist())
                .andExpect(jsonPath("$.totalCLP").value(255320));
    }

    @Test
    void obtenerBoletaPorReserva_cuandoNoExiste_retorna404() throws Exception {
        when(pagoService.obtenerBoletaPorReserva(eq(999L), any()))
                .thenThrow(new RecursoNoEncontradoException("Boleta no encontrada").conCodigo("BOLETA_NO_ENCONTRADA"));

        mockMvc.perform(get("/api/v1/pagos/reservas/999/boleta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOLETA_NO_ENCONTRADA"));
    }

    @Test
    void descargarBoletaPdf_retornaPdfConHeadersCorrectos() throws Exception {
        byte[] pdfFake = "%PDF-1.4 Fake PDF bytes".getBytes();
        when(pagoService.generarBoletaPdf(eq(1021L), any())).thenReturn(new BoletaPdfDto(2458731L, pdfFake));

        mockMvc.perform(get("/api/v1/pagos/boletas/1021/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"boleta-2458731.pdf\""))
                .andExpect(content().bytes(pdfFake));
    }

    @Test
    void descargarBoletaPdf_conCredencialDeCompraDeInvitado_laEntregaAlServicio() throws Exception {
        byte[] pdfFake = "%PDF-1.4 Fake".getBytes();
        when(pagoService.generarBoletaPdf(1021L, "TOKEN-INVITADO")).thenReturn(new BoletaPdfDto(2458731L, pdfFake));

        mockMvc.perform(get("/api/v1/pagos/boletas/1021/pdf").header("X-Compra-Token", "TOKEN-INVITADO"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfFake));
    }

}
