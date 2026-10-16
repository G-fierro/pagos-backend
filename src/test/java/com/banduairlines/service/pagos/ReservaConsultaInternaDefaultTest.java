package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.ReservaParaPago;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaConsultaInternaDefaultTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PasajeroRepository pasajeroRepository;

    private Pasajero pasajero(Long id, TipoPasajero tipo, String nombre, TipoDocumento doc, String numero) {
        return Pasajero.builder().id(id).tipo(tipo).nombre(nombre).apellido("Soto")
                .tipoDocumento(doc).numeroDocumento(numero).build();
    }

    @Test
    void compraDeInvitado_usaPrimerAdultoParaElNombreYNoInventaDatos() {
        Reserva reserva = Reserva.builder().id(5L).codigoReserva("K7QX2M").estado(EstadoReserva.PENDIENTE_PAGO)
                .emailContacto("javiera@test.com")
                .subtotal(BigDecimal.valueOf(300000)).descuento(BigDecimal.valueOf(44680)).total(BigDecimal.valueOf(255320))
                .expiraEn(LocalDateTime.of(2026, 9, 27, 1, 0)).build();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(pasajeroRepository.findByReservaIdOrderByIdAsc(5L)).thenReturn(List.of(
                pasajero(1L, TipoPasajero.NINO, "Matías", TipoDocumento.OTRO, "X1"),
                pasajero(2L, TipoPasajero.ADULTO, "Javiera", TipoDocumento.RUT, "18.245.673-4")));

        ReservaParaPago info = new ReservaConsultaInternaDefault(reservaRepository, pasajeroRepository).obtenerParaPago(5L);

        assertEquals("Javiera Soto", info.contacto().nombreCompleto());
        assertEquals("javiera@test.com", info.contacto().correo());
        assertNull(info.contacto().telefono());
        assertEquals(2, info.pasajeros().size());
        assertEquals(300000, info.total().lineas().get(0).montoCLP());
        assertEquals(44680, info.total().descuentoCLP());
        assertEquals(255320, info.total().totalCLP());
    }

}
