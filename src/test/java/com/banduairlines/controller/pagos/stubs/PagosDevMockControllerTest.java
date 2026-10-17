package com.banduairlines.controller.pagos.stubs;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.pagos.stubs.DevJwtAuthHelper;
import com.banduairlines.service.pagos.stubs.DevReservaMockService;

@ExtendWith(MockitoExtension.class)
class PagosDevMockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DevReservaMockService devReservaMockService;

    @Mock
    private DevJwtAuthHelper devJwtAuthHelper;

    @Mock
    private ReservaRepository reservaRepository;

    private PagosDevMockController controller;

    @BeforeEach
    void setUp() {
        controller = new PagosDevMockController(devReservaMockService, devJwtAuthHelper, reservaRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sembrarReservas_retorna200() throws Exception {
        doNothing().when(devReservaMockService).sembrarTodasLasReservas();

        mockMvc.perform(post("/api/v1/pagos/dev/reservas/sembrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.reservas").isArray());

        verify(devReservaMockService).sembrarTodasLasReservas();
    }

    @Test
    void resetearReserva_retorna200() throws Exception {
        Reserva r = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.PENDIENTE_PAGO)
                .expiraEn(LocalDateTime.now().plusDays(2))
                .build();
        when(devReservaMockService.restablecerReserva(1L)).thenReturn(r);

        mockMvc.perform(post("/api/v1/pagos/dev/reservas/1/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(1L))
                .andExpect(jsonPath("$.codigoReserva").value("K7QX2M"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));

        verify(devReservaMockService).restablecerReserva(1L);
    }

    @Test
    void obtenerTokenDev_retorna200ConToken() throws Exception {
        when(devJwtAuthHelper.generarToken(1L, "pasajero@test.com", RolUsuario.PASAJERO))
                .thenReturn("jwt.token.de.prueba");

        mockMvc.perform(get("/api/v1/pagos/dev/auth/token")
                        .param("rol", "PASAJERO")
                        .param("usuarioId", "1")
                        .param("email", "pasajero@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.de.prueba"))
                .andExpect(jsonPath("$.tokenTipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("PASAJERO"));
    }

    @Test
    void listarReservas_retornaLista() throws Exception {
        Reserva r = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.PENDIENTE_PAGO)
                .total(BigDecimal.valueOf(255320))
                .expiraEn(LocalDateTime.now().plusDays(2))
                .build();
        when(reservaRepository.findAll()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/pagos/dev/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].codigo").value("K7QX2M"))
                .andExpect(jsonPath("$[0].totalCLP").value(255320));
    }
}
