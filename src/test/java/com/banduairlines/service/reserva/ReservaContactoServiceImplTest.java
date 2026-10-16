package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaContactoServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaContactoServiceImplTest {

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;

    private ReservaContactoServiceImpl service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ReservaContactoServiceImpl(reservaAcceso, reservaRepository);
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        reserva.setEmailContacto(null);
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
    }

    @Test
    void registrar_guardaElContactoConElCorreoNormalizado() {
        ContactoResponse r = service.registrar(1L, "tok",
                new ContactoRequest("  Tomás Rojas ", " Tomas.Rojas@Correo.CL ", "+56 9 1234 5678"));

        assertThat(r).isEqualTo(new ContactoResponse("Tomás Rojas", "tomas.rojas@correo.cl", "+56 9 1234 5678"));
        assertThat(reserva.getEmailContacto()).isEqualTo("tomas.rojas@correo.cl");
        assertThat(reserva.getNombreContacto()).isEqualTo("Tomás Rojas");
        verify(reservaRepository).save(reserva);
    }

    @Test
    void registrar_reservaQueYaNoEstaEnCurso_noGuarda() {
        doThrow(new ConflictoException("La reserva está PENDIENTE_PAGO").conCodigo("ESTADO_INVALIDO"))
                .when(reservaAcceso).exigirEnCurso(reserva);

        assertThatThrownBy(() -> service.registrar(1L, "tok", new ContactoRequest("Tomás", "t@correo.cl", "+56912345678")))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ESTADO_INVALIDO");
        verify(reservaRepository, never()).save(any());
    }
}
