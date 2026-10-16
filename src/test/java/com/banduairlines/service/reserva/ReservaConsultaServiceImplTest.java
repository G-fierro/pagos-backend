package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Ruta;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.impl.ReservaConsultaServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaConsultaServiceImplTest {

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private TramoReservaRepository tramoReservaRepository;
    @Mock private PasajeroRepository pasajeroRepository;
    @Mock private BoletoRepository boletoRepository;

    private ReservaConsultaServiceImpl service;
    private Reserva reserva;
    private TramoReserva tramo;

    @BeforeEach
    void setUp() {
        service = new ReservaConsultaServiceImpl(reservaAcceso, tramoReservaRepository, pasajeroRepository,
                boletoRepository, new ResumenReserva(new CalculadoraTotalReserva()));
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        reserva.setCodigoReserva(null);
        reserva.setEmailContacto(null);
        reserva.setCantidadAdultos(2);
        reserva.setCantidadNinos(0);
        reserva.setCantidadBebes(0);

        Ruta ruta = Ruta.builder().origen(Aeropuerto.builder().codigoIata("SCL").build())
                .destino(Aeropuerto.builder().codigoIata("PMC").build()).build();
        Vuelo vuelo = Vuelo.builder().id(10L).codigoVuelo("BA-110").ruta(ruta)
                .salidaProgramada(ReservaFixtures.AHORA.plusDays(3)).llegadaProgramada(ReservaFixtures.AHORA.plusDays(3).plusHours(2))
                .build();
        TarifaVuelo tarifa = TarifaVuelo.builder().id(100L).vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY)
                .pack(ReservaFixtures.packLight()).precio(BigDecimal.valueOf(29990)).build();
        tramo = TramoReserva.builder().id(1L).reserva(reserva).vuelo(vuelo).tarifaVuelo(tarifa).orden(1)
                .precioTarifa(BigDecimal.valueOf(29990)).build();

        when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        when(tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(1L)).thenReturn(List.of(tramo));
    }

    @Test
    void obtener_antesDeRegistrarPasajeros_muestraItinerarioYTotalInicial() {
        when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of());
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of());

        ReservaResponse r = service.obtener(1L, "tok");

        assertThat(r.codigoReserva()).isNull();
        assertThat(r.estado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(r.pack()).isEqualTo("Light");
        assertThat(r.itinerario()).singleElement().satisfies(t -> {
            assertThat(t.origen()).isEqualTo("SCL");
            assertThat(t.destino()).isEqualTo("PMC");
            assertThat(t.cabina()).isEqualTo(ClaseCabina.ECONOMY);
        });
        assertThat(r.pasajeros()).isEmpty();
        assertThat(r.contacto()).isNull();
        assertThat(r.promocion()).isNull();
        assertThat(r.total().totalCLP()).isEqualTo(59980);
        assertThat(r.total().lineas().get(0).descripcion()).isEqualTo("Tarifas (2 boletos)");
    }

    @Test
    void obtener_conPasajerosYPromocion_usaLaCalculadora() {
        Pasajero a1 = Pasajero.builder().id(1L).tipo(TipoPasajero.ADULTO).nombre("Ana").apellido("Pérez").build();
        Pasajero a2 = Pasajero.builder().id(2L).tipo(TipoPasajero.ADULTO).nombre("Luis").apellido("Pérez").build();
        Boleto b1 = Boleto.builder().id(1L).reserva(reserva).pasajeroId(a1).vueloId(tramo.getVuelo())
                .tarifaVueloId(tramo.getTarifaVuelo()).precioTarifa(BigDecimal.valueOf(29990)).build();
        Boleto b2 = Boleto.builder().id(2L).reserva(reserva).pasajeroId(a2).vueloId(tramo.getVuelo())
                .tarifaVueloId(tramo.getTarifaVuelo()).precioTarifa(BigDecimal.valueOf(29990)).build();
        reserva.setPromocion(ReservaFixtures.promocion("BANDU10", TipoDescuento.PORCENTAJE, 10));
        reserva.setDescuento(BigDecimal.valueOf(5998));
        reserva.setEmailContacto("ana@correo.cl");
        when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of(a1, a2));
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(b1, b2));

        ReservaResponse r = service.obtener(1L, "tok");

        assertThat(r.pasajeros()).hasSize(2);
        assertThat(r.asientos()).hasSize(2).allSatisfy(a -> assertThat(a.asiento()).isNull());
        assertThat(r.promocion().codigo()).isEqualTo("BANDU10");
        assertThat(r.contacto().correo()).isEqualTo("ana@correo.cl");
        assertThat(r.total().descuentoCLP()).isEqualTo(5998);
        assertThat(r.total().totalCLP()).isEqualTo(59980 - 5998);
    }
}
