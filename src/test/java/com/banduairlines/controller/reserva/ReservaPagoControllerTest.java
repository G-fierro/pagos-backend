package com.banduairlines.controller.reserva;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.IniciarPagoRequest;
import com.banduairlines.dto.reserva.ReservaCanceladaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.reserva.ReservaCancelacionService;
import com.banduairlines.service.reserva.ReservaPagoService;

@ExtendWith(MockitoExtension.class)
class ReservaPagoControllerTest {

    @Mock private ReservaPagoService reservaPagoService;
    @Mock private ReservaCancelacionService reservaCancelacionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaPagoController(reservaPagoService, reservaCancelacionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void iniciarPago_sinTotalEsperado_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas/1/pago").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.details[0].campo").value("totalEsperado"));
    }

    @Test
    void iniciarPago_precioCambiado_responde409ConSuCodigo() throws Exception {
        when(reservaPagoService.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L)))
                .thenThrow(new ConflictoException("El precio de tu vuelo cambió.").conCodigo("PRECIO_CAMBIADO"));

        mockMvc.perform(post("/api/v1/reservas/1/pago").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalEsperado\":187980}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRECIO_CAMBIADO"));
    }

    @Test
    void abandonarCompra_respondeLaReservaCancelada() throws Exception {
        when(reservaCancelacionService.cancelar(1L, "tok"))
                .thenReturn(new ReservaCanceladaResponse(1L, null, EstadoReserva.CANCELADA));

        mockMvc.perform(post("/api/v1/reservas/1/cancelacion").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
