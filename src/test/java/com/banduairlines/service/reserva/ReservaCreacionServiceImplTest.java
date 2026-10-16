package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Avion;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.enums.Sentido;
import com.banduairlines.repository.CatalogoCompraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.impl.ReservaCreacionServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservaCreacionServiceImplTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private TramoReservaRepository tramoReservaRepository;
    @Mock private CatalogoCompraRepository catalogo;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioActual usuarioActual;
    @Mock private ReservaAcceso reservaAcceso;

    private final CompraTokenService compraTokenService = new CompraTokenService("clave_de_prueba_de_al_menos_32_caracteres");
    private ReservaCreacionServiceImpl service;

    private PackTarifario light;
    private Vuelo ida;
    private Vuelo vuelta;

    @BeforeEach
    void setUp() {
        service = new ReservaCreacionServiceImpl(reservaRepository, tramoReservaRepository, catalogo, usuarioRepository,
                usuarioActual, compraTokenService, reservaAcceso, 15);

        light = ReservaFixtures.packLight();
        light.setActivo(true);
        Avion avion = Avion.builder().id(7L).build();
        ida = Vuelo.builder().id(10L).codigoVuelo("BA-110").aeronave(avion).estado(EstadoVuelo.PROGRAMADO)
                .salidaProgramada(ReservaFixtures.AHORA.plusDays(3)).build();
        vuelta = Vuelo.builder().id(11L).codigoVuelo("BA-111").aeronave(avion).estado(EstadoVuelo.PROGRAMADO)
                .salidaProgramada(ReservaFixtures.AHORA.plusDays(6)).build();

        lenient().when(reservaAcceso.ahora()).thenReturn(ReservaFixtures.AHORA);
        lenient().when(usuarioActual.obtener()).thenReturn(Optional.empty());
        lenient().when(catalogo.buscarPackPorNombre("Light")).thenReturn(Optional.of(light));
        lenient().when(catalogo.bloquearVuelo(10L)).thenReturn(Optional.of(ida));
        lenient().when(catalogo.bloquearVuelo(11L)).thenReturn(Optional.of(vuelta));
        lenient().when(catalogo.buscarTarifa(10L, ClaseCabina.ECONOMY, 1L))
                .thenReturn(Optional.of(tarifa(ida, 29990)));
        lenient().when(catalogo.buscarTarifa(11L, ClaseCabina.ECONOMY, 1L))
                .thenReturn(Optional.of(tarifa(vuelta, 31990)));
        lenient().when(catalogo.contarAsientos(7L, ClaseCabina.ECONOMY)).thenReturn(66L);
        lenient().when(tramoReservaRepository.contarPlazasTomadas(any(), any(), any(), anyCollection(), any()))
                .thenReturn(0L);
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });
    }

    @Test
    void crear_invitadoIdaYVuelta_quedaEnCursoConTokenTotalYTramosCongelados() {
        ReservaCreadaResponse r = service.crear(request(2, 1, 1, tramo(10L, null), tramo(11L, Sentido.VUELTA)));

        assertThat(r.reservaId()).isEqualTo(42L);
        assertThat(r.estado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(compraTokenService.esValido(42L, r.compraToken())).isTrue();
        assertThat(r.expiraEn().toLocalDateTime()).isEqualTo(ReservaFixtures.AHORA.plusMinutes(15));
        // 4 pasajeros (bebé incluido) pagan tarifa completa en los 2 tramos: (29990 + 31990) * 4
        assertThat(r.total().totalCLP()).isEqualTo(247920);
        assertThat(r.total().lineas()).singleElement()
                .satisfies(l -> assertThat(l.descripcion()).isEqualTo("Tarifas (8 boletos)"));

        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reserva.capture());
        assertThat(reserva.getValue().getUsuario()).isNull();
        assertThat(reserva.getValue().getEmailContacto()).isNull();
        assertThat(reserva.getValue().getCantidadAdultos()).isEqualTo(2);
        assertThat(reserva.getValue().getCantidadBebes()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TramoReserva>> tramos = ArgumentCaptor.forClass(List.class);
        verify(tramoReservaRepository).saveAll(tramos.capture());
        assertThat(tramos.getValue()).extracting(TramoReserva::getOrden).containsExactly(1, 2);
        assertThat(tramos.getValue()).extracting(TramoReserva::getSentido).containsExactly(Sentido.IDA, Sentido.VUELTA);
        assertThat(tramos.getValue().get(1).getPrecioTarifa()).isEqualByComparingTo("31990");
        assertThat(tramos.getValue()).allSatisfy(t -> assertThat(t.getReserva().getId()).isEqualTo(42L));
    }

    @Test
    void crear_pasajeroConSesion_asociaLaCuentaYNoEntregaToken() {
        when(usuarioActual.obtener()).thenReturn(Optional.of(new UsuarioAutenticado(5L, "ana@correo.cl", RolUsuario.PASAJERO)));
        when(usuarioRepository.getReferenceById(5L)).thenReturn(Usuario.builder().id(5L).build());

        ReservaCreadaResponse r = service.crear(request(1, 0, 0, tramo(10L, null)));

        assertThat(r.compraToken()).isNull();
        assertThat(r.total().totalCLP()).isEqualTo(29990);
        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reserva.capture());
        assertThat(reserva.getValue().getUsuario().getId()).isEqualTo(5L);
    }

    @Test
    void crear_masBebesQueAdultos_lanzaBebesSinAdultoSinTocarLaBase() {
        assertThatThrownBy(() -> service.crear(request(1, 0, 2, tramo(10L, null))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("BEBES_SIN_ADULTO");

        verifyNoInteractions(catalogo, reservaRepository, tramoReservaRepository);
    }

    @Test
    void crear_tramoInexistente_lanza404() {
        when(catalogo.bloquearVuelo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, tramo(10L, null), tramo(99L, null))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_vueloCancelado_lanzaSinDisponibilidad() {
        ida.setEstado(EstadoVuelo.CANCELADO);

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, tramo(10L, null))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("SIN_DISPONIBILIDAD");
    }

    @Test
    void crear_vueloYaSalio_lanzaSinDisponibilidad() {
        ida.setSalidaProgramada(ReservaFixtures.AHORA.minusMinutes(1));

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, tramo(10L, null))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("SIN_DISPONIBILIDAD");
    }

    @Test
    void crear_tarifaNoOfrecidaEnLaCabina_lanzaSinDisponibilidad() {
        assertThatThrownBy(() -> service.crear(new CrearReservaRequest(null,
                List.of(new CrearReservaRequest.Tramo(10L, ClaseCabina.BUSINESS, null)), "Light", cantidad(1, 0, 0))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("SIN_DISPONIBILIDAD");
    }

    @Test
    void crear_packInexistenteOInactivo_lanzaSinDisponibilidad() {
        light.setActivo(false);

        assertThatThrownBy(() -> service.crear(request(1, 0, 0, tramo(10L, null))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("SIN_DISPONIBILIDAD");
    }

    @Test
    void crear_sinCupoEnCabina_lanzaSinDisponibilidadYLosBebesNoCuentan() {
        when(tramoReservaRepository.contarPlazasTomadas(eq(10L), eq(ClaseCabina.ECONOMY), any(), anyCollection(), any()))
                .thenReturn(65L);

        // 1 adulto + 1 bebé = 1 plaza: cabe justo
        assertThat(service.crear(request(1, 0, 1, tramo(10L, null))).estado()).isEqualTo(EstadoReserva.EN_CURSO);
        // 2 adultos = 2 plazas: no cabe
        assertThatThrownBy(() -> service.crear(request(2, 0, 0, tramo(10L, null))))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("SIN_DISPONIBILIDAD");
    }

    private static TarifaVuelo tarifa(Vuelo vuelo, long precio) {
        return TarifaVuelo.builder().id(vuelo.getId() * 10).vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY)
                .precio(BigDecimal.valueOf(precio)).build();
    }

    private static CrearReservaRequest.Tramo tramo(Long vueloId, Sentido sentido) {
        return new CrearReservaRequest.Tramo(vueloId, ClaseCabina.ECONOMY, sentido);
    }

    private static CrearReservaRequest.CantidadPasajeros cantidad(int adultos, int ninos, int bebes) {
        return new CrearReservaRequest.CantidadPasajeros(adultos, ninos, bebes);
    }

    private static CrearReservaRequest request(int adultos, int ninos, int bebes, CrearReservaRequest.Tramo... tramos) {
        return new CrearReservaRequest("ALT-1", List.of(tramos), "Light", cantidad(adultos, ninos, bebes));
    }
}
