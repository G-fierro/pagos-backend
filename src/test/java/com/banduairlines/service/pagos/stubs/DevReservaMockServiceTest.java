package com.banduairlines.service.pagos.stubs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.model.Pago;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DevReservaMockServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PasajeroRepository pasajeroRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private DevReservaMockService service;

    @BeforeEach
    void setUp() {
        service = new DevReservaMockService(reservaRepository, pasajeroRepository, pagoRepository, usuarioRepository);
    }

    @Test
    void restablecerReserva_limpiaPagosPreviosYRenuevaVigencia() {
        Reserva reserva = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.CANCELADA)
                .expiraEn(LocalDateTime.now().minusDays(1))
                .build();

        Pago pagoPrevio = Pago.builder().id(10L).reserva(reserva).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(pagoPrevio));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva resultado = service.restablecerReserva(1L);

        verify(pagoRepository).deleteAll(List.of(pagoPrevio));
        assertEquals(EstadoReserva.PENDIENTE_PAGO, resultado.getEstado());
        assertTrue(resultado.getExpiraEn().isAfter(LocalDateTime.now()));
    }

    @Test
    void confirmarReserva_cambiaEstadoAConfirmada() {
        Reserva reserva = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.PENDIENTE_PAGO)
                .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva resultado = service.confirmarReserva(1L);

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
    }

    @Test
    void cancelarReserva_cambiaEstadoACancelada() {
        Reserva reserva = Reserva.builder()
                .id(1L)
                .codigoReserva("K7QX2M")
                .estado(EstadoReserva.PENDIENTE_PAGO)
                .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva resultado = service.cancelarReserva(1L);

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
    }
}
