package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.PaginaResponse;
import com.banduairlines.dto.pagos.PagoRegistroResponse;
import com.banduairlines.dto.pagos.ResumenPagosResponse;
import com.banduairlines.dto.pagos.TotalMedioPagoDto;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.service.pagos.PagoService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PagoAdminController pagoAdminController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoAdminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void obtenerResumen_retorna200ConMetricas() throws Exception {
        ResumenPagosResponse resumen = new ResumenPagosResponse(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                2,
                300000L,
                150000L,
                1,
                List.of(new TotalMedioPagoDto("Tarjeta de Crédito Visa", 2, 300000L))
        );
        when(pagoService.obtenerResumenReportes(any(), any())).thenReturn(resumen);

        mockMvc.perform(get("/api/v1/admin/pagos/resumen")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPagos").value(2))
                .andExpect(jsonPath("$.montoTotalCLP").value(300000))
                .andExpect(jsonPath("$.ticketPromedioCLP").value(150000))
                .andExpect(jsonPath("$.cantidadRechazados").value(1))
                .andExpect(jsonPath("$.porMedioPago[0].medioPago").value("Tarjeta de Crédito Visa"));
    }

    @Test
    void listarPagos_retorna200ConPagina() throws Exception {
        PagoRegistroResponse registro = new PagoRegistroResponse(
                10L, 1L, "K7QX2M", EstadoPago.PAGADO, 255320, "Tarjeta de Crédito Visa", Instant.now(), Instant.now()
        );
        PaginaResponse<PagoRegistroResponse> pagina = new PaginaResponse<>(
                List.of(registro), 0, 10, 1L, 1
        );
        when(pagoService.obtenerPagosReportes(any(), any(), anyInt(), anyInt())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/admin/pagos")
                        .param("pagina", "0")
                        .param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].pagoId").value(10L))
                .andExpect(jsonPath("$.contenido[0].codigoReserva").value("K7QX2M"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }
}
