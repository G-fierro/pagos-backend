package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.PaginaResponse;
import com.banduairlines.dto.pagos.PagoRegistroResponse;
import com.banduairlines.dto.pagos.ResumenPagosResponse;
import com.banduairlines.dto.pagos.TotalMedioPagoResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.service.pagos.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private AdminPagoController adminPagoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPagoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/pagos/resumen retorna 200 con ResumenPagosResponse")
    void obtenerResumen_retorna200() throws Exception {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        TotalMedioPagoResponse medio = new TotalMedioPagoResponse("Tarjeta de Crédito", 5, 1250000);
        ResumenPagosResponse resumen = new ResumenPagosResponse(
                desde, hasta, 5, 1250000, 250000, 1, List.of(medio)
        );

        when(pagoService.obtenerResumenAdmin(any(), any())).thenReturn(resumen);

        mockMvc.perform(get("/api/v1/admin/pagos/resumen")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPagos").value(5))
                .andExpect(jsonPath("$.montoTotalCLP").value(1250000))
                .andExpect(jsonPath("$.ticketPromedioCLP").value(250000))
                .andExpect(jsonPath("$.cantidadRechazados").value(1))
                .andExpect(jsonPath("$.porMedioPago[0].medioPago").value("Tarjeta de Crédito"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/pagos retorna 200 con PaginaResponse de PagoRegistroResponse")
    void listarPagos_retorna200() throws Exception {
        PagoRegistroResponse item = new PagoRegistroResponse(
                1L, 10L, "K7QX2M", EstadoPago.PAGADO, 255320, "Webpay",
                Instant.parse("2026-10-14T10:00:00Z"), Instant.parse("2026-10-14T09:55:00Z")
        );
        PaginaResponse<PagoRegistroResponse> pagina = new PaginaResponse<>(
                List.of(item), 0, 10, 1L, 1
        );

        when(pagoService.listarPagosAdmin(eq(0), eq(10))).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/admin/pagos")
                        .param("pagina", "0")
                        .param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].pagoId").value(1))
                .andExpect(jsonPath("$.contenido[0].codigoReserva").value("K7QX2M"));
    }
}
