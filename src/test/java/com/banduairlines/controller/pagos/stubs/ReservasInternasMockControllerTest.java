package com.banduairlines.controller.pagos.stubs;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.pagos.stubs.DevReservaMockService;

@ExtendWith(MockitoExtension.class)
class ReservasInternasMockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DevReservaMockService devReservaMockService;

    private ReservasInternasMockController controller;

    @BeforeEach
    void setUp() {
        controller = new ReservasInternasMockController(devReservaMockService, "clave_compartida_reservas");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recibirResultadoPago_conPagoAprobado_confirmaReservaYRetorna200() throws Exception {
        Reserva reservaConfirmada = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.CONFIRMADA)
                .build();
        when(devReservaMockService.confirmarReserva(1L)).thenReturn(reservaConfirmada);

        String payload = """
                {
                    "pagoId": 10,
                    "estado": "PAGADO",
                    "medioPago": "Webpay",
                    "monto": 255320,
                    "fechaPago": "2026-10-17T01:00:00Z",
                    "flowOrder": 11107650
                }
                """;

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .header("X-Service-Key", "clave_compartida_reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.nuevoEstado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.codigoReserva").value("K7QX2M"));

        verify(devReservaMockService).confirmarReserva(1L);
    }

    @Test
    void recibirResultadoPago_conPagoRechazado_cancelaReservaYRetorna200() throws Exception {
        Reserva reservaCancelada = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.CANCELADA)
                .build();
        when(devReservaMockService.cancelarReserva(1L)).thenReturn(reservaCancelada);

        String payload = """
                {
                    "pagoId": 10,
                    "estado": "RECHAZADO",
                    "medioPago": "Webpay",
                    "monto": 255320,
                    "fechaPago": "2026-10-17T01:00:00Z",
                    "flowOrder": 11107650
                }
                """;

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .header("X-Service-Key", "clave_compartida_reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.nuevoEstado").value("CANCELADA"));

        verify(devReservaMockService).cancelarReserva(1L);
    }

    @Test
    void recibirResultadoPago_sinServiceKey_retorna401() throws Exception {
        String payload = """
                {
                    "pagoId": 10,
                    "estado": "PAGADO",
                    "medioPago": "Webpay",
                    "monto": 255320
                }
                """;

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recibirResultadoPago_conServiceKeyInvalida_retorna401() throws Exception {
        String payload = """
                {
                    "pagoId": 10,
                    "estado": "PAGADO",
                    "medioPago": "Webpay",
                    "monto": 255320
                }
                """;

        mockMvc.perform(post("/api/v1/internas/reservas/1/resultado-pago")
                        .header("X-Service-Key", "clave_invalida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
