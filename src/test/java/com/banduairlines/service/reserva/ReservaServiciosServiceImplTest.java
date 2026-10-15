package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.CatalogoServiciosResponse;
import com.banduairlines.dto.reserva.ContratarServiciosRequest;
import com.banduairlines.dto.reserva.ServiciosContratadosResponse;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaServiciosServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaServiciosServiceImplTest {

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private ExtraRepository extraRepository;

    private ReservaServiciosServiceImpl service;
    private Reserva reserva;
    private Boleto boleto;
    private final Extra bodega = ReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 24990);
    private final Extra cabina = ReservaFixtures.extra(2, TipoExtra.EQUIPAJE_CABINA, 9990);
    private final Extra embarque = ReservaFixtures.extra(3, TipoExtra.EMBARQUE_PRIORITARIO, 4990);
    private final Extra asiento = ReservaFixtures.extra(4, TipoExtra.ASIENTO_PREFERENTE, 6500);

    @BeforeEach
    void setUp() {
        service = new ReservaServiciosServiceImpl(reservaAcceso, reservaRepository, boletoRepository,
                extraRepository, new CalculadoraTotalReserva());
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        boleto = ReservaFixtures.boleto(reserva, 1, 10, 59990, ReservaFixtures.packLight());
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boleto));
        when(extraRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(bodega, cabina, embarque, asiento));
    }

    @Test
    void obtenerCatalogo_noOfreceLoQueLaTarifaIncluyeNiAsientos() {
        CatalogoServiciosResponse catalogo = service.obtenerCatalogo(1L, "tok");

        assertThat(catalogo.incluidos()).extracting("tipo").containsExactly("ARTICULO_PERSONAL", "EQUIPAJE_CABINA");
        assertThat(catalogo.disponibles()).extracting("servicioId").containsExactly("1", "3");
        assertThat(catalogo.disponibles().get(0).maximoPorPasajero()).isEqualTo(3);
    }

    @Test
    void contratar_reemplazaServiciosSinTocarElAsientoYActualizaElTotal() {
        boleto.addExtra(embarque, 1, BigDecimal.valueOf(4990));
        boleto.addExtra(asiento, 1, BigDecimal.valueOf(6500));
        ContratarServiciosRequest request = new ContratarServiciosRequest(List.of(
                new ContratarServiciosRequest.Servicio("1", 1L, 10L, 1),
                new ContratarServiciosRequest.Servicio("1", 1L, 10L, 1)));

        ServiciosContratadosResponse r = service.contratar(1L, "tok", request);

        assertThat(r.servicios()).singleElement().satisfies(s -> {
            assertThat(s.servicioId()).isEqualTo("1");
            assertThat(s.cantidad()).isEqualTo(2);
            assertThat(s.subtotalCLP()).isEqualTo(49980);
        });
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo())
                .containsExactlyInAnyOrder(TipoExtra.ASIENTO_PREFERENTE, TipoExtra.EQUIPAJE_BODEGA);
        assertThat(r.total().totalCLP()).isEqualTo(59990 + 6500 + 49980);
        assertThat(reserva.getTotal()).isEqualByComparingTo("116470");
        verify(reservaRepository).save(reserva);
    }

    @Test
    void contratar_superaElMaximo_lanzaServicioNoPermitido() {
        ContratarServiciosRequest request = new ContratarServiciosRequest(List.of(
                new ContratarServiciosRequest.Servicio("3", 1L, 10L, 2)));

        assertThatThrownBy(() -> service.contratar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("SERVICIO_NO_PERMITIDO");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void contratar_servicioIncluidoEnLaTarifa_lanzaServicioNoPermitido() {
        ContratarServiciosRequest request = new ContratarServiciosRequest(List.of(
                new ContratarServiciosRequest.Servicio("2", 1L, 10L, 1)));

        assertThatThrownBy(() -> service.contratar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no aplica a la tarifa Light");
    }

    @Test
    void contratar_pasajeroSinBoletoEnElTramo_lanzaServicioNoPermitido() {
        ContratarServiciosRequest request = new ContratarServiciosRequest(List.of(
                new ContratarServiciosRequest.Servicio("1", 1L, 99L, 1)));

        assertThatThrownBy(() -> service.contratar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no tiene un boleto en el tramo 99");
    }
}
