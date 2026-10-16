package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.impl.ReservaPasajerosServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaPasajerosServiceImplTest {

    /** Salida del primer tramo: 2026-11-11. */
    private static final LocalDate FECHA_VUELO = ReservaFixtures.AHORA.plusDays(10).toLocalDate();

    @Mock private ReservaAcceso reservaAcceso;
    @Mock private ReservaRepository reservaRepository;
    @Mock private TramoReservaRepository tramoReservaRepository;
    @Mock private PasajeroRepository pasajeroRepository;
    @Mock private BoletoRepository boletoRepository;
    @Mock private ExtraRepository extraRepository;

    private ReservaPasajerosServiceImpl service;
    private Reserva reserva;
    private TramoReserva tramo;
    private long secuencia;

    @BeforeEach
    void setUp() {
        service = new ReservaPasajerosServiceImpl(reservaAcceso, reservaRepository, tramoReservaRepository,
                pasajeroRepository, boletoRepository, new AsientosReserva(extraRepository), new CalculadoraTotalReserva());
        reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        reserva.setCantidadAdultos(2);
        reserva.setCantidadNinos(1);
        reserva.setCantidadBebes(1);

        Vuelo vuelo = Vuelo.builder().id(10L).codigoVuelo("BA-110").salidaProgramada(FECHA_VUELO.atTime(9, 0)).build();
        TarifaVuelo tarifa = TarifaVuelo.builder().id(100L).vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY)
                .pack(ReservaFixtures.packLight()).precio(BigDecimal.valueOf(29990)).build();
        tramo = TramoReserva.builder().id(1L).reserva(reserva).vuelo(vuelo).tarifaVuelo(tarifa).orden(1)
                .precioTarifa(BigDecimal.valueOf(29990)).build();

        secuencia = 1;
        lenient().when(reservaAcceso.obtenerAutorizada(1L, "tok")).thenReturn(reserva);
        lenient().when(tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(1L)).thenReturn(List.of(tramo));
        lenient().when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of());
        lenient().when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of());
        lenient().when(pasajeroRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            List<Pasajero> guardados = new ArrayList<>();
            for (Object o : (Iterable<?>) inv.getArgument(0)) {
                Pasajero p = (Pasajero) o;
                if (p.getId() == null) {
                    p.setId(secuencia++);
                }
                guardados.add(p);
            }
            return guardados;
        });
        lenient().when(boletoRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            List<Boleto> guardados = new ArrayList<>();
            ((Iterable<?>) inv.getArgument(0)).forEach(o -> guardados.add((Boleto) o));
            return guardados;
        });
    }

    @Test
    void registrar_primeraVez_creaPasajerosUnBoletoPorTramoYEnlazaResponsables() {
        PasajerosRegistradosResponse r = service.registrar(1L, "tok", familia());

        assertThat(r.pasajeros()).extracting(PasajeroResponse::ref).containsExactly("a1", "a2", "n1", "b1");
        assertThat(r.pasajeros()).allSatisfy(p -> assertThat(p.pasajeroId()).isNotNull());
        Long idA1 = r.pasajeros().get(0).pasajeroId();
        assertThat(r.pasajeros().get(2).adultoResponsableId()).isEqualTo(idA1);
        assertThat(r.pasajeros().get(3).adultoResponsableId()).isEqualTo(idA1);
        assertThat(r.pasajeros().get(0).nacionalidad()).isEqualTo("Chilena");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Boleto>> boletos = ArgumentCaptor.forClass(List.class);
        verify(boletoRepository).saveAll(boletos.capture());
        assertThat(boletos.getValue()).hasSize(4)
                .allSatisfy(b -> {
                    assertThat(b.getVueloId().getId()).isEqualTo(10L);
                    assertThat(b.getPrecioTarifa()).isEqualByComparingTo("29990");
                });
        // 4 pasajeros (bebé incluido) × 29.990
        assertThat(reserva.getTotal()).isEqualByComparingTo("119960");
        verify(reservaRepository).save(reserva);
    }

    @Test
    void registrar_cantidadDistintaALaDeclarada_lanzaCantidadPasajerosDistinta() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("CANTIDAD_PASAJEROS_DISTINTA");
        verify(pasajeroRepository, never()).saveAll(anyIterable());
    }

    @Test
    void registrar_adultoQueEnElVueloTieneDiezAnios_lanzaTipoNoCoincideEdad() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"),
                pasajero("a2", TipoPasajero.ADULTO, FECHA_VUELO.minusYears(10), "22222222-2", null),
                nino("n1", "a1"), bebe("b1", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("TIPO_NO_COINCIDE_EDAD");
    }

    @Test
    void registrar_edadSeCalculaAlPrimerTramo_quienCumple12ElDiaDelVueloEsAdulto() {
        reserva.setCantidadAdultos(1);
        reserva.setCantidadNinos(0);
        reserva.setCantidadBebes(0);
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                pasajero("a1", TipoPasajero.ADULTO, FECHA_VUELO.minusYears(12), "11111111-1", null)));

        assertThat(service.registrar(1L, "tok", request).pasajeros()).hasSize(1);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(FECHA_VUELO.minusYears(12).plusDays(1), FECHA_VUELO))
                .isEqualTo(TipoPasajero.NINO);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(FECHA_VUELO.minusYears(2), FECHA_VUELO))
                .isEqualTo(TipoPasajero.NINO);
        assertThat(ReservaPasajerosServiceImpl.tipoSegunEdad(FECHA_VUELO.minusYears(2).plusDays(1), FECHA_VUELO))
                .isEqualTo(TipoPasajero.BEBE);
    }

    @Test
    void registrar_bebeSinAdultoResponsable_lanzaMenorSinAdultoResponsable() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", "a1"), bebe("b1", null)));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE");
    }

    @Test
    void registrar_ninoConAdultosEnLaReservaSinResponsable_lanzaMenorSinAdultoResponsable() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", null), bebe("b1", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE");
    }

    @Test
    void registrar_responsableQueNoEsAdulto_lanzaMenorSinAdultoResponsable() {
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", "a1"), bebe("b1", "n1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE");
    }

    @Test
    void registrar_dosBebesConElMismoAdulto_lanzaMenorSinAdultoResponsable() {
        reserva.setCantidadNinos(0);
        reserva.setCantidadBebes(2);
        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), bebe("b1", "a1"),
                pasajero("b2", TipoPasajero.BEBE, FECHA_VUELO.minusMonths(3), "44444444-4", "a1")));

        assertThatThrownBy(() -> service.registrar(1L, "tok", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("MENOR_SIN_ADULTO_RESPONSABLE");
    }

    @Test
    void registrar_ninoQueViajaSoloSinAdultosEnLaReserva_noExigeResponsable() {
        reserva.setCantidadAdultos(0);
        reserva.setCantidadNinos(1);
        reserva.setCantidadBebes(0);

        PasajerosRegistradosResponse r = service.registrar(1L, "tok",
                new RegistrarPasajerosRequest(List.of(nino("n1", null))));

        assertThat(r.pasajeros()).singleElement().satisfies(p -> assertThat(p.adultoResponsableId()).isNull());
    }

    @Test
    void registrar_reenvio_actualizaLosMismosRegistrosYConservaAsientos() {
        Pasajero a1 = existente(1L, TipoPasajero.ADULTO, null);
        Pasajero a2 = existente(2L, TipoPasajero.ADULTO, null);
        Pasajero n1 = existente(3L, TipoPasajero.NINO, a1);
        Pasajero b1 = existente(4L, TipoPasajero.BEBE, a1);
        when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of(a1, a2, n1, b1));
        Boleto boletoNino = boleto(n1, Asiento.builder().id(77L).fila(5).letra("B").build());
        Boleto boletoAdulto = boleto(a1, Asiento.builder().id(76L).fila(5).letra("A").build());
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boletoAdulto, boletoNino));

        PasajerosRegistradosResponse r = service.registrar(1L, "tok", familia());

        assertThat(r.pasajeros()).extracting(PasajeroResponse::pasajeroId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(a1.getNombre()).isEqualTo("Ana");
        assertThat(boletoNino.getAsientoId()).isNotNull();
        assertThat(boletoAdulto.getAsientoId()).isNotNull();
        verify(boletoRepository, never()).saveAll(anyIterable());
    }

    @Test
    void registrar_reenvioCambiaElResponsableDelNino_liberaSuAsiento() {
        Pasajero a1 = existente(1L, TipoPasajero.ADULTO, null);
        Pasajero a2 = existente(2L, TipoPasajero.ADULTO, null);
        Pasajero n1 = existente(3L, TipoPasajero.NINO, a1);
        Pasajero b1 = existente(4L, TipoPasajero.BEBE, a1);
        when(pasajeroRepository.findByReserva_IdOrderByIdAsc(1L)).thenReturn(List.of(a1, a2, n1, b1));
        Boleto boletoNino = boleto(n1, Asiento.builder().id(77L).fila(5).letra("B").build());
        when(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(1L)).thenReturn(List.of(boletoNino));

        RegistrarPasajerosRequest request = new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", "a2"), bebe("b1", "a1")));
        service.registrar(1L, "tok", request);

        assertThat(n1.getAdultoResponsable()).isSameAs(a2);
        assertThat(boletoNino.getAsientoId()).isNull();
    }

    @Test
    void registrar_reservaVencida_propagaElErrorSinGuardar() {
        doThrow(new ConflictoException("Venció el tiempo").conCodigo("RESERVA_EXPIRADA"))
                .when(reservaAcceso).exigirEnCurso(reserva);

        assertThatThrownBy(() -> service.registrar(1L, "tok", familia()))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("RESERVA_EXPIRADA");
        verify(pasajeroRepository, never()).saveAll(anyIterable());
        verify(reservaRepository, never()).save(any());
    }

    // ------------------------------------------------------------ datos

    private static RegistrarPasajerosRequest familia() {
        return new RegistrarPasajerosRequest(List.of(
                adulto("a1", "11111111-1"), adulto("a2", "22222222-2"), nino("n1", "a1"), bebe("b1", "a1")));
    }

    private static RegistrarPasajerosRequest.Pasajero adulto(String ref, String documento) {
        return pasajero(ref, TipoPasajero.ADULTO, LocalDate.of(1990, 3, 15), documento, null);
    }

    private static RegistrarPasajerosRequest.Pasajero nino(String ref, String responsable) {
        return pasajero(ref, TipoPasajero.NINO, FECHA_VUELO.minusYears(8), "33333333-3", responsable);
    }

    private static RegistrarPasajerosRequest.Pasajero bebe(String ref, String responsable) {
        return pasajero(ref, TipoPasajero.BEBE, FECHA_VUELO.minusMonths(8), "55555555-5", responsable);
    }

    private static RegistrarPasajerosRequest.Pasajero pasajero(String ref, TipoPasajero tipo, LocalDate nacimiento,
                                                              String documento, String responsable) {
        return new RegistrarPasajerosRequest.Pasajero(ref, tipo, "Ana", "Pérez", nacimiento, TipoDocumento.RUT,
                documento, "Chilena", responsable);
    }

    private Pasajero existente(Long id, TipoPasajero tipo, Pasajero responsable) {
        return Pasajero.builder().id(id).reserva(reserva).tipo(tipo).nombre("Antiguo").apellido("Nombre")
                .tipoDocumento(TipoDocumento.RUT).numeroDocumento("doc-" + id).fechaNacimiento(LocalDate.of(1990, 1, 1))
                .adultoResponsable(responsable).build();
    }

    private Boleto boleto(Pasajero pasajero, Asiento asiento) {
        return Boleto.builder().id(pasajero.getId() * 100).reserva(reserva).pasajeroId(pasajero)
                .vueloId(tramo.getVuelo()).tarifaVueloId(tramo.getTarifaVuelo()).asientoId(asiento)
                .precioTarifa(BigDecimal.valueOf(29990)).build();
    }
}
