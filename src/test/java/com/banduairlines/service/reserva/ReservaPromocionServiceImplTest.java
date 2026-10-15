package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.AplicarPromocionRequest;
import com.banduairlines.dto.reserva.PromocionAplicadaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PromocionRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.impl.ReservaPromocionServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaPromocionServiceImplTest {

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private PromocionRepository promocionRepository;

    private ReservaPromocionServiceImpl service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ReservaPromocionServiceImpl(reservaAcceso, reservaRepository, boletoRepository,
                promocionRepository, new CalculadoraTotalReserva());
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(reservaAcceso.ahora()).thenReturn(ReservaFixtures.AHORA);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(
                ReservaFixtures.boleto(reserva, 1, 10, 187980, ReservaFixtures.packLight())));
    }

    @Test
    void aplicar_codigoValido_guardaLaPromocionYElDescuento() {
        Promocion promo = ReservaFixtures.promocion("BANDU10", TipoDescuento.PORCENTAJE, 10);
        when(promocionRepository.findByCodigoIgnoreCase("bandu10")).thenReturn(Optional.of(promo));

        PromocionAplicadaResponse r = service.aplicar(1L, "tok", new AplicarPromocionRequest(" bandu10 "));

        assertThat(r.promocion().descuentoCLP()).isEqualTo(18798);
        assertThat(r.total().totalCLP()).isEqualTo(169182);
        assertThat(reserva.getPromocion()).isSameAs(promo);
    }

    @Test
    void aplicar_codigoInexistenteOInactivo_lanzaPromocionInvalida() {
        Promocion inactiva = ReservaFixtures.promocion("OFF", TipoDescuento.PORCENTAJE, 10);
        inactiva.setActiva(false);
        when(promocionRepository.findByCodigoIgnoreCase("OFF")).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.aplicar(1L, "tok", new AplicarPromocionRequest("OFF")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("PROMOCION_INVALIDA");
    }

    @Test
    void aplicar_fueraDeVigencia_lanzaPromocionVencida() {
        Promocion vieja = ReservaFixtures.promocion("VIEJA", TipoDescuento.MONTO_FIJO, 5000);
        vieja.setVigenteHasta(ReservaFixtures.AHORA.minusDays(1));
        when(promocionRepository.findByCodigoIgnoreCase("VIEJA")).thenReturn(Optional.of(vieja));

        assertThatThrownBy(() -> service.aplicar(1L, "tok", new AplicarPromocionRequest("VIEJA")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PROMOCION_VENCIDA");
    }

    @Test
    void aplicar_sinUsosDisponibles_lanzaPromocionNoAplica() {
        Promocion agotada = ReservaFixtures.promocion("AGOTADA", TipoDescuento.PORCENTAJE, 10);
        agotada.setUsosMaximos(5);
        agotada.setUsosActuales(5);
        when(promocionRepository.findByCodigoIgnoreCase("AGOTADA")).thenReturn(Optional.of(agotada));

        assertThatThrownBy(() -> service.aplicar(1L, "tok", new AplicarPromocionRequest("AGOTADA")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PROMOCION_NO_APLICA");
    }

    @Test
    void quitar_recalculaSinDescuento() {
        reserva.setPromocion(ReservaFixtures.promocion("BANDU10", TipoDescuento.PORCENTAJE, 10));

        assertThat(service.quitar(1L, "tok").total().totalCLP()).isEqualTo(187980);
        assertThat(reserva.getPromocion()).isNull();
    }
}
