package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.MedioPagoDto;
import com.banduairlines.dto.pagos.PagoDetalleResponse;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PagoController pagoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void obtenerPago_cuandoExiste_retorna200() throws Exception {
        PagoDetalleResponse response = new PagoDetalleResponse(
                10L, 1L, "K7QX2M", EstadoPago.PAGADO, 255320,
                new MedioPagoDto("Tarjeta de Crédito Visa", "4521"),
                3849571L, Instant.now(), null, 10L, false);
        when(pagoService.obtenerPago(eq(10L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/pagos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagoId").value(10L))
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.montoCLP").value(255320))
                .andExpect(jsonPath("$.medioPago.nombre").value("Tarjeta de Crédito Visa"))
                .andExpect(jsonPath("$.medioPago.ultimosDigitos").value("4521"))
                .andExpect(jsonPath("$.puedeReintentar").value(false));
    }

    @Test
    void obtenerPago_cuandoNoExiste_retorna404() throws Exception {
        when(pagoService.obtenerPago(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Pago no encontrado: 99").conCodigo("PAGO_NO_ENCONTRADO"));

        mockMvc.perform(get("/api/v1/pagos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PAGO_NO_ENCONTRADO"));
    }

    @Test
    void notificaciones_cuandoRecibeToken_retorna200() throws Exception {
        doNothing().when(pagoService).procesarNotificacionFlow("TOKEN_XYZ");

        mockMvc.perform(post("/api/v1/pagos/notificaciones")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "TOKEN_XYZ"))
                .andExpect(status().isOk());

        verify(pagoService).procesarNotificacionFlow("TOKEN_XYZ");
    }

    @Test
    void retorno_cuandoRecibeToken_redirigeCon303() throws Exception {
        when(pagoService.procesarRetornoFlow("TOKEN_XYZ")).thenReturn("/pago/resultado?pagoId=10");

        mockMvc.perform(post("/api/v1/pagos/retorno")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "TOKEN_XYZ"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/pago/resultado?pagoId=10"));
    }

    @Test
    void reembolsosNotificaciones_cuandoRecibeToken_retorna200() throws Exception {
        doNothing().when(pagoService).procesarNotificacionReembolso("REF_TOKEN_XYZ");

        mockMvc.perform(post("/api/v1/pagos/reembolsos/notificaciones")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "REF_TOKEN_XYZ"))
                .andExpect(status().isOk());

        verify(pagoService).procesarNotificacionReembolso("REF_TOKEN_XYZ");
    }
}