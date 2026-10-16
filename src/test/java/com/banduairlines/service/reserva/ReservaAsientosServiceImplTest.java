package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.AsignacionAsiento;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoCompraRepository;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.impl.ReservaAsientosServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaAsientosServiceImplTest {

    private static final long TARIFA = 29990;
    private static final long PREFERENTE = 5000;

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private TramoReservaRepository tramoReservaRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private CatalogoCompraRepository catalogo;
    @Mock private ExtraRepository extraRepository;

    private ReservaAsientosServiceImpl service;
    private Reserva reserva;
    private Vuelo vuelo;
    private PackTarifario pack;
    private TarifaVuelo tarifa;
    private Pasajero adulto;
    private Pasajero nino;
    private Pasajero bebe;
    private Boleto boletoAdulto;
    private Boleto boletoNino;
    private Boleto boletoBebe;
    private List<Asiento> asientos;
    private List<Extra> extras;

    @BeforeEach
    void setUp() {
        service = new ReservaAsientosServiceImpl(reservaAcceso, reservaRepository, tramoReservaRepository, boletoRepository,
                catalogo, new AsientosReserva(extraRepository), new CalculadoraTotalReserva());
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        pack = ReservaFixtures.packLight(); // sin selección de asiento incluida
        vuelo = Vuelo.builder().id(10L).codigoVuelo("BA-110").aeronave(Avion.builder().id(7L).build()).build();
        tarifa = TarifaVuelo.builder().id(100L).vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(pack)
                .precio(BigDecimal.valueOf(TARIFA)).build();

        adulto = Pasajero.builder().id(1L).tipo(TipoPasajero.ADULTO).nombre("Ana").fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
        nino = Pasajero.builder().id(2L).tipo(TipoPasajero.NINO).nombre("Leo").adultoResponsable(adulto).build();
        bebe = Pasajero.builder().id(3L).tipo(TipoPasajero.BEBE).nombre("Sol").adultoResponsable(adulto).build();
        boletoAdulto = boleto(adulto);
        boletoNino = boleto(nino);
        boletoBebe = boleto(bebe);

        asientos = new ArrayList<>();
        for (String letra : List.of("A", "C", "D", "F")) {
            asientos.add(asiento(fila(1, letra), 1, letra, ClaseCabina.BUSINESS, true));
        }
        for (int fila : List.of(5, 6, 10)) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(asiento(fila(fila, letra), fila, letra, ClaseCabina.ECONOMY, fila == 10));
            }
        }
        extras = new ArrayList<>(List.of(
                ReservaFixtures.extra(60, TipoExtra.EQUIPAJE_BODEGA, 15000),
                ReservaFixtures.extra(51, TipoExtra.ASIENTO_PREFERENTE, PREFERENTE)));

        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(reservaAcceso.ahora()).thenReturn(ReservaFixtures.AHORA);
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L))
                .thenReturn(List.of(boletoAdulto, boletoNino, boletoBebe));
        lenient().when(catalogo.asientosDeAeronave(7L)).thenReturn(asientos);
        lenient().when(extraRepository.findByActivoTrueOrderByIdAsc()).thenAnswer(inv -> extras);
        lenient().when(catalogo.bloquearVuelo(10L)).thenReturn(Optional.of(vuelo));
        lenient().when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), anyCollection(), any()))
                .thenReturn(List.of());
        lenient().when(tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(1L)).thenReturn(List.of(
                TramoReserva.builder().id(1L).reserva(reserva).vuelo(vuelo).tarifaVuelo(tarifa).orden(1)
                        .precioTarifa(BigDecimal.valueOf(TARIFA)).build()));
    }

    // ---------------------------------------------------------------- endpoint 6

    @Test
    void seleccionar_adultoYNinoJuntosEnAsientosNormales_noCobraYBloquea() {
        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5b")));

        assertThat(r.asignaciones()).extracting(AsignacionAsiento::asiento).containsExactly("5A", "5B", null);
        assertThat(r.asignaciones()).extracting(AsignacionAsiento::precioCLP).containsExactly(0L, 0L, 0L);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
        assertThat(r.expiraEn()).isNotNull();
        assertThat(boletoAdulto.getAsientoId().getId()).isEqualTo(fila(5, "A"));
        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(reserva.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(3 * TARIFA));
        verify(catalogo).bloquearVuelo(10L);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void seleccionar_preferenteEnPackSinSeleccion_cobraElExtraPreferente() {
        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "10A")));

        assertThat(r.asignaciones().get(0).precioCLP()).isEqualTo(PREFERENTE);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA + PREFERENTE);
        assertThat(boletoAdulto.getExtras()).extracting(be -> be.getExtra().getTipo())
                .containsExactly(TipoExtra.ASIENTO_PREFERENTE);
    }

    @Test
    void seleccionar_packConSeleccionIncluida_preferenteTambienEsGratis() {
        pack.setIncluyeSeleccionAsiento(true);

        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, "10A"), sel(2, "10B")));

        assertThat(r.asignaciones()).extracting(AsignacionAsiento::precioCLP).containsExactly(0L, 0L, 0L);
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
        assertThat(boletoAdulto.getExtras()).isEmpty();
    }

    @Test
    void seleccionar_cambiarDeAsiento_reemplazaLosExtrasSinDuplicar() {
        service.seleccionar(1L, "tok", request(sel(1, "10A"), sel(2, "10B")));
        service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(boletoAdulto.getAsientoId().getFila()).isEqualTo(5);
    }

    @Test
    void seleccionar_asientoNulo_liberaElAsientoYSuCobro() {
        service.seleccionar(1L, "tok", request(sel(1, "10A")));
        assertThat(boletoAdulto.getExtras()).hasSize(1);

        AsientosAsignadosResponse r = service.seleccionar(1L, "tok", request(sel(1, null)));

        assertThat(boletoAdulto.getAsientoId()).isNull();
        assertThat(boletoAdulto.getExtras()).isEmpty();
        assertThat(r.total().totalCLP()).isEqualTo(3 * TARIFA);
    }

    @Test
    void seleccionar_ninoAntesQueSuAdulto_lanzaAdultoSinAsiento() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(2, "5B"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ADULTO_SIN_ASIENTO");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void seleccionar_ninoAlOtroLadoDelPasillo_lanzaMenorSeparadoDeAdulto() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5C"), sel(2, "5D"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
    }

    @Test
    void seleccionar_ninoEnOtraFila_lanzaMenorSeparadoDeAdulto() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "6A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
    }

    @Test
    void seleccionar_adultoSeCambiaYDejaAlNinoAtras_lanzaMenorSeparadoDeAdulto() {
        service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "6A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SEPARADO_DE_ADULTO");
    }

    @Test
    void seleccionar_adultoLiberaSuAsientoConElNinoSentado_lanzaAdultoSinAsiento() {
        service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5B")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, null))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ADULTO_SIN_ASIENTO");
    }

    @Test
    void seleccionar_asientoParaBebe_lanzaBebeNoOcupaAsiento() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(3, "5B"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("BEBE_NO_OCUPA_ASIENTO");
    }

    @Test
    void seleccionar_asientoTomadoPorOtraCompra_lanzaAsientoNoDisponible() {
        when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), anyCollection(), any()))
                .thenReturn(List.of(fila(5, "A")));

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_asientoDeOtraCabinaOInexistente_lanzaAsientoNoDisponible() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "1A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "40Z"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_mismoAsientoParaDosPasajeros_lanzaAsientoNoDisponible() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"), sel(2, "5A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
    }

    @Test
    void seleccionar_preferenteSinPrecioEnElCatalogo_lanzaAsientoNoDisponibleYElNormalSigueGratis() {
        extras.removeIf(e -> e.getTipo() == TipoExtra.ASIENTO_PREFERENTE);

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "10A"))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ASIENTO_NO_DISPONIBLE");
        assertThat(service.seleccionar(1L, "tok", request(sel(1, "5A"))).asignaciones().get(0).precioCLP()).isZero();
    }

    @Test
    void seleccionar_pasajeroOTramoAjeno_lanza404() {
        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(new SeleccionarAsientosRequest.Seleccion(99L, 10L, "5A"))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(catalogo, never()).bloquearVuelo(anyLong());
    }

    @Test
    void seleccionar_sinPasajerosRegistrados_lanzaDatosIncompletos() {
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.seleccionar(1L, "tok", request(sel(1, "5A"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("DATOS_INCOMPLETOS");
    }

    // ---------------------------------------------------------------- endpoint 5

    @Test
    void obtenerMapa_marcaEstadosPreciosYDistribucion() {
        boletoAdulto.setAsientoId(buscar(5, "A"));
        when(boletoRepository.asientosTomadosPorOtras(eq(10L), eq(1L), any(), anyCollection(), any()))
                .thenReturn(List.of(fila(5, "C")));

        MapaAsientosResponse mapa = service.obtenerMapa(1L, 10L, "tok");

        assertThat(mapa.cabina()).isEqualTo(ClaseCabina.ECONOMY);
        assertThat(mapa.columnas()).containsExactly("A", "B", "C", "D", "E", "F");
        assertThat(mapa.pasillosDespuesDe()).containsExactly("C");
        assertThat(mapa.filas()).extracting(MapaAsientosResponse.Fila::numero).containsExactly(1, 5, 6, 10);
        assertThat(asiento(mapa, "5A").estado()).isEqualTo("SELECCIONADO");
        assertThat(asiento(mapa, "5B").estado()).isEqualTo("DISPONIBLE");
        assertThat(asiento(mapa, "5B").precioCLP()).isZero();
        assertThat(asiento(mapa, "5B").incluidoEnPack()).isTrue();
        assertThat(asiento(mapa, "5C").estado()).isEqualTo("NO_DISPONIBLE");
        assertThat(asiento(mapa, "1A").estado()).isEqualTo("NO_DISPONIBLE");
        assertThat(asiento(mapa, "10A").categoria()).isEqualTo("PREFERENTE");
        assertThat(asiento(mapa, "10A").precioCLP()).isEqualTo(PREFERENTE);
        assertThat(asiento(mapa, "10A").incluidoEnPack()).isFalse();
    }

    @Test
    void obtenerMapa_packConSeleccionIncluida_todoIncluidoYGratis() {
        pack.setIncluyeSeleccionAsiento(true);

        MapaAsientosResponse mapa = service.obtenerMapa(1L, 10L, "tok");

        assertThat(asiento(mapa, "5B").incluidoEnPack()).isTrue();
        assertThat(asiento(mapa, "5B").precioCLP()).isZero();
        assertThat(asiento(mapa, "10A").incluidoEnPack()).isTrue();
        assertThat(asiento(mapa, "10A").precioCLP()).isZero();
    }

    @Test
    void obtenerMapa_tramoQueNoEsDeLaReserva_lanza404() {
        assertThatThrownBy(() -> service.obtenerMapa(1L, 99L, "tok"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------------------------------------------------------------- datos

    private static long fila(int fila, String letra) {
        return fila * 10L + (letra.charAt(0) - 'A');
    }

    private static Asiento asiento(long id, int fila, String letra, ClaseCabina cabina, boolean preferente) {
        return Asiento.builder().id(id).fila(fila).letra(letra).claseCabina(cabina).esPreferente(preferente).build();
    }

    private Asiento buscar(int fila, String letra) {
        return asientos.stream().filter(a -> a.getFila() == fila && a.getLetra().equals(letra)).findFirst().orElseThrow();
    }

    private static MapaAsientosResponse.Asiento asiento(MapaAsientosResponse mapa, String codigo) {
        return mapa.filas().stream().flatMap(f -> f.asientos().stream())
                .filter(a -> a.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private Boleto boleto(Pasajero pasajero) {
        return Boleto.builder().id(pasajero.getId() * 100).reserva(reserva).pasajeroId(pasajero)
                .vueloId(vuelo).tarifaVueloId(tarifa).precioTarifa(BigDecimal.valueOf(TARIFA))
                .extras(new ArrayList<BoletoExtra>()).build();
    }

    private static SeleccionarAsientosRequest.Seleccion sel(long pasajeroId, String asiento) {
        return new SeleccionarAsientosRequest.Seleccion(pasajeroId, 10L, asiento);
    }

    private static SeleccionarAsientosRequest request(SeleccionarAsientosRequest.Seleccion... selecciones) {
        return new SeleccionarAsientosRequest(Arrays.asList(selecciones));
    }
}
