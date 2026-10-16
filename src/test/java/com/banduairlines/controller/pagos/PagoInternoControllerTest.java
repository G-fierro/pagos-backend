package com.banduairlines.controller.pagos;

import com.banduairlines.dto.pagos.IniciarPagoRequest;
import com.banduairlines.dto.pagos.PagoIniciadoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.service.pagos.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoInternoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PagoInternoController pagoInternoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoInternoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void iniciarPago_cuandoEsValido_retorna201() throws Exception {
        PagoIniciadoResponse response = new PagoIniciadoResponse(
                10L, "K7QX2M", EstadoPago.PENDIENTE, "https://sandbox.flow.cl/app/web/pay.php?token=TOKEN123", 255320, Instant.now().plusSeconds(1800)
        );
        when(pagoService.iniciarPago(any(IniciarPagoRequest.class), eq("clave_compartida"))).thenReturn(response);

        String json = """
                {
                    "reservaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/internas/pagos")
                        .header("X-Service-Key", "clave_compartida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagoId").value(10L))
                .andExpect(jsonPath("$.codigoReserva").value("K7QX2M"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.montoCLP").value(255320));
    }

    @Test
    void iniciarPago_cuandoFaltaReservaId_retorna400() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/v1/internas/pagos")
                        .header("X-Service-Key", "clave_compartida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.details[?(@.campo == 'reservaId')]").exists());
    }

    @Test
    void iniciarPago_cuandoEstadoInvalido_retorna409() throws Exception {
        when(pagoService.iniciarPago(any(), any()))
                .thenThrow(new ConflictoException("La reserva no está en PENDIENTE_PAGO").conCodigo("ESTADO_INVALIDO"));

        String json = """
                {
                    "reservaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/internas/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ESTADO_INVALIDO"));
    }

    @Test
    void iniciarPago_cuandoMontoInvalido_retorna422() throws Exception {
        when(pagoService.iniciarPago(any(), any()))
                .thenThrow(new ReglaNegocioException("Monto es menor o igual a cero").conCodigo("MONTO_INVALIDO"));

        String json = """
                {
                    "reservaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/internas/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("MONTO_INVALIDO"));
    }
}
