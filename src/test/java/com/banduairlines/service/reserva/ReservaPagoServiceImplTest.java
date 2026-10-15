package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.IniciarPagoRequest;
import com.banduairlines.dto.reserva.PagoIniciadoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaPagoServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaPagoServiceImplTest {

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private PasajeroRepository pasajeroRepository;
    @Mock private CobroReservaService cobroReservaService;

    private ReservaPagoServiceImpl service;
    private Reserva reserva;
    private Boleto boleto;

    @BeforeEach
    void setUp() {
        service = new ReservaPagoServiceImpl(reservaAcceso, reservaRepository, boletoRepository,
                pasajeroRepository, new CalculadoraTotalReserva(), cobroReservaService, 15);
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        reserva.setCodigoReserva(null);
        boleto = ReservaFixtures.boleto(reserva, 1, 10, 187980, ReservaFixtures.packLight());
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(reservaAcceso.ahora()).thenReturn(ReservaFixtures.AHORA);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boleto));
        lenient().when(pasajeroRepository.countByReserva_Id(1L)).thenReturn(1L);
        lenient().when(cobroReservaService.iniciarCobro(any(), anyLong(), anyString()))
                .thenReturn(new CobroReservaService.CobroIniciado("PAG-000123", "https://sandbox.flow.cl/pay"));
    }

    @Test
    void iniciarPago_totalCoincide_generaCodigoPasaAPendientePagoYPideElCobro() {
        PagoIniciadoResponse r = service.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L));

        assertThat(r.estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(r.codigoReserva()).matches("[A-Z0-9]{6}");
        assertThat(r.urlPago()).isEqualTo("https://sandbox.flow.cl/pay");
        assertThat(r.montoCLP()).isEqualTo(187980);
        assertThat(reserva.getExpiraEn()).isEqualTo(ReservaFixtures.AHORA.plusMinutes(15));
        verify(cobroReservaService).iniciarCobro(reserva, 187980, "tomas@correo.cl");
    }

    @Test
    void iniciarPago_precioSubio_lanzaPrecioCambiadoYGuardaElPrecioVigente() {
        boleto.getTarifaVueloId().setPrecio(BigDecimal.valueOf(195980));

        assertThatThrownBy(() -> service.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Total nuevo: 195980")
                .extracting("codigo").isEqualTo("PRECIO_CAMBIADO");

        assertThat(boleto.getPrecioTarifa()).isEqualByComparingTo("195980");
        assertThat(reserva.getTotal()).isEqualByComparingTo("195980");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        verify(cobroReservaService, never()).iniciarCobro(any(), anyLong(), anyString());
    }

    @Test
    void iniciarPago_reservaConfirmada_lanzaEstadoInvalido() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        assertThatThrownBy(() -> service.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L)))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void iniciarPago_sinContacto_lanzaDatosIncompletos() {
        reserva.setEmailContacto(" ");

        assertThatThrownBy(() -> service.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("DATOS_INCOMPLETOS");
    }

    @Test
    void iniciarPago_reservaVencida_lanzaReservaExpirada() {
        doThrow(new ConflictoException("vencida").conCodigo("RESERVA_EXPIRADA"))
                .when(reservaAcceso).exigirNoExpirada(eq(reserva));

        assertThatThrownBy(() -> service.iniciarPago(1L, "tok", new IniciarPagoRequest(187980L)))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("RESERVA_EXPIRADA");
    }
}
