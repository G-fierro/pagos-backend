package com.banduairlines.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.Sentido;
import com.banduairlines.repository.CatalogoCompraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.CompraTokenService;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaCreacionService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaCreacionServiceImpl implements ReservaCreacionService {

    static final String SIN_DISPONIBILIDAD = "SIN_DISPONIBILIDAD";
    static final String BEBES_SIN_ADULTO = "BEBES_SIN_ADULTO";

    /** Un vuelo ya embarcando, en vuelo, aterrizado o cancelado no se vende. */
    private static final Set<EstadoVuelo> VUELOS_VENDIBLES = Set.of(EstadoVuelo.PROGRAMADO, EstadoVuelo.RETRASADO);

    private final ReservaRepository reservaRepository;
    private final TramoReservaRepository tramoReservaRepository;
    private final CatalogoCompraRepository catalogo;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final CompraTokenService compraTokenService;
    private final ReservaAcceso reservaAcceso;
    private final long minutosBloqueo;

    public ReservaCreacionServiceImpl(ReservaRepository reservaRepository, TramoReservaRepository tramoReservaRepository,
                                      CatalogoCompraRepository catalogo,
                                      UsuarioRepository usuarioRepository, UsuarioActual usuarioActual,
                                      CompraTokenService compraTokenService, ReservaAcceso reservaAcceso,
                                      @Value("${app.reserva.minutos-bloqueo:15}") long minutosBloqueo) {
        this.reservaRepository = reservaRepository;
        this.tramoReservaRepository = tramoReservaRepository;
        this.catalogo = catalogo;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.compraTokenService = compraTokenService;
        this.reservaAcceso = reservaAcceso;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Valida lo que no depende de la base primero (bebés por adulto), luego vuelo, tarifa y cupo por tramo.
     * Solo si todo es válido se escribe: la reserva queda EN_CURSO con sus tramos y precios congelados.
     */
    @Override
    @Transactional
    public ReservaCreadaResponse crear(CrearReservaRequest request) {
        CrearReservaRequest.CantidadPasajeros cantidad = request.cantidadPasajeros();
        if (cantidad.bebes() > cantidad.adultos()) {
            throw new ReglaNegocioException("Cada bebé debe viajar con un adulto: hay " + cantidad.bebes()
                    + " bebés y " + cantidad.adultos() + " adultos")
                    .conCodigo(BEBES_SIN_ADULTO);
        }

        LocalDateTime ahora = reservaAcceso.ahora();
        PackTarifario pack = buscarPack(request.pack());
        Map<Long, Vuelo> vuelos = buscarVuelos(request.tramos());

        List<TramoReserva> tramos = new ArrayList<>();
        int orden = 1;
        for (CrearReservaRequest.Tramo pedido : request.tramos()) {
            Vuelo vuelo = vuelos.get(pedido.tramoId());
            exigirVueloVendible(vuelo, ahora);
            TarifaVuelo tarifa = catalogo
                    .buscarTarifa(vuelo.getId(), pedido.cabina(), pack.getId())
                    .orElseThrow(() -> new ConflictoException("El vuelo " + vuelo.getCodigoVuelo()
                            + " no ofrece la tarifa " + pack.getNombre() + " en cabina " + pedido.cabina())
                            .conCodigo(SIN_DISPONIBILIDAD));
            exigirCupo(vuelo, tarifa, cantidad.conAsiento(), ahora);

            tramos.add(TramoReserva.builder()
                    .vuelo(vuelo)
                    .tarifaVuelo(tarifa)
                    .sentido(pedido.sentido() != null ? pedido.sentido() : Sentido.IDA)
                    .orden(orden++)
                    .precioTarifa(tarifa.getPrecio())
                    .build());
        }

        long porPasajero = tramos.stream().mapToLong(t -> CalculadoraTotalReserva.clp(t.getPrecioTarifa())).sum();
        long total = porPasajero * cantidad.total();

        UsuarioAutenticado usuario = usuarioActual.obtener().orElse(null);
        Reserva reserva = Reserva.builder()
                .usuario(usuario != null ? usuarioRepository.getReferenceById(usuario.id()) : null)
                .estado(EstadoReserva.EN_CURSO)
                .cantidadAdultos(cantidad.adultos())
                .cantidadNinos(cantidad.ninos())
                .cantidadBebes(cantidad.bebes())
                .subtotal(BigDecimal.valueOf(total))
                .descuento(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(total))
                .expiraEn(ahora.plusMinutes(minutosBloqueo))
                .build();
        reserva = reservaRepository.save(reserva);

        for (TramoReserva tramo : tramos) {
            tramo.setReserva(reserva);
        }
        tramoReservaRepository.saveAll(tramos);

        // Solo el invitado necesita credencial de compra; el pasajero registrado usa su JWT.
        String compraToken = usuario == null ? compraTokenService.generar(reserva.getId()) : null;

        return new ReservaCreadaResponse(
                reserva.getId(),
                reserva.getEstado(),
                compraToken,
                reserva.getExpiraEn().atOffset(ZoneOffset.UTC),
                ResumenReserva.totalInicial(cantidad.total() * tramos.size(), total));
    }

    private PackTarifario buscarPack(String nombre) {
        return catalogo.buscarPackPorNombre(nombre.trim())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ConflictoException("La tarifa " + nombre.trim() + " no está disponible")
                        .conCodigo(SIN_DISPONIBILIDAD));
    }

    /**
     * Carga y bloquea los vuelos (en orden de id, para no generar interbloqueos) hasta el fin de la transacción:
     * así dos compras simultáneas no pueden tomar el mismo último cupo.
     */
    private Map<Long, Vuelo> buscarVuelos(List<CrearReservaRequest.Tramo> tramos) {
        Map<Long, Vuelo> vuelos = new HashMap<>();
        tramos.stream().map(CrearReservaRequest.Tramo::tramoId).sorted().forEach(id -> vuelos.put(id,
                catalogo.bloquearVuelo(id).orElseThrow(() -> new RecursoNoEncontradoException("Tramo", id))));
        return vuelos;
    }

    private void exigirVueloVendible(Vuelo vuelo, LocalDateTime ahora) {
        if (!VUELOS_VENDIBLES.contains(vuelo.getEstado()) || !vuelo.getSalidaProgramada().isAfter(ahora)) {
            throw new ConflictoException("El vuelo " + vuelo.getCodigoVuelo() + " ya no está a la venta")
                    .conCodigo(SIN_DISPONIBILIDAD);
        }
    }

    /** Asientos de la cabina en la aeronave menos las plazas que ya tienen otras compras vigentes. */
    private void exigirCupo(Vuelo vuelo, TarifaVuelo tarifa, int plazasPedidas, LocalDateTime ahora) {
        if (plazasPedidas == 0) {
            return;
        }
        long capacidad = catalogo.contarAsientos(vuelo.getAeronave().getId(), tarifa.getClaseCabina());
        long tomadas = tramoReservaRepository.contarPlazasTomadas(
                vuelo.getId(), tarifa.getClaseCabina(), EstadoReserva.CONFIRMADA, ReservaAcceso.ABIERTAS, ahora);
        if (tomadas + plazasPedidas > capacidad) {
            throw new ConflictoException("No quedan " + plazasPedidas + " asientos en cabina " + tarifa.getClaseCabina()
                    + " del vuelo " + vuelo.getCodigoVuelo())
                    .conCodigo(SIN_DISPONIBILIDAD);
        }
    }
}
