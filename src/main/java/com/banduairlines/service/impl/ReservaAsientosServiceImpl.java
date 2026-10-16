package com.banduairlines.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.CatalogoCompraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.reserva.AsientosReserva;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaAsientosService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaAsientosServiceImpl implements ReservaAsientosService {

    static final String ASIENTO_NO_DISPONIBLE = "ASIENTO_NO_DISPONIBLE";
    static final String ADULTO_SIN_ASIENTO = "ADULTO_SIN_ASIENTO";
    static final String MENOR_SEPARADO_DE_ADULTO = "MENOR_SEPARADO_DE_ADULTO";
    static final String BEBE_NO_OCUPA_ASIENTO = "BEBE_NO_OCUPA_ASIENTO";
    static final String DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS";

    static final String DISPONIBLE = "DISPONIBLE";
    static final String SELECCIONADO = "SELECCIONADO";
    static final String NO_DISPONIBLE = "NO_DISPONIBLE";

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final TramoReservaRepository tramoReservaRepository;
    private final BoletoRepository boletoRepository;
    private final CatalogoCompraRepository catalogo;
    private final AsientosReserva asientosReserva;
    private final CalculadoraTotalReserva calculadora;

    public ReservaAsientosServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                      TramoReservaRepository tramoReservaRepository, BoletoRepository boletoRepository,
                                      CatalogoCompraRepository catalogo,
                                      AsientosReserva asientosReserva, CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.tramoReservaRepository = tramoReservaRepository;
        this.boletoRepository = boletoRepository;
        this.catalogo = catalogo;
        this.asientosReserva = asientosReserva;
        this.calculadora = calculadora;
    }

    // ---------------------------------------------------------------- endpoint 5

    /**
     * Vendidos o bloqueados por otra compra vigente, de otra cabina o sin precio configurado → NO_DISPONIBLE;
     * los de esta reserva → SELECCIONADO. precioCLP es lo que cuesta elegirlo con el pack de la reserva.
     */
    @Override
    @Transactional(readOnly = true)
    public MapaAsientosResponse obtenerMapa(Long reservaId, Long tramoId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        TramoReserva tramo = tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(reserva.getId()).stream()
                .filter(t -> t.getVuelo().getId().equals(tramoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Tramo", tramoId));

        Vuelo vuelo = tramo.getVuelo();
        ClaseCabina cabina = tramo.getTarifaVuelo().getClaseCabina();
        PackTarifario pack = tramo.getTarifaVuelo().getPack();
        Map<TipoExtra, Extra> precios = asientosReserva.preciosVigentes();
        List<Asiento> asientos = catalogo.asientosDeAeronave(vuelo.getAeronave().getId());

        Set<Long> tomados = new HashSet<>(asientosTomadosPorOtras(vuelo.getId(), reserva.getId()));
        Set<Long> propios = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId()).stream()
                .filter(b -> b.getVueloId().getId().equals(vuelo.getId()) && b.getAsientoId() != null)
                .map(b -> b.getAsientoId().getId())
                .collect(Collectors.toSet());

        Map<Integer, List<MapaAsientosResponse.Asiento>> porFila = new LinkedHashMap<>();
        Map<Integer, ClaseCabina> cabinaPorFila = new HashMap<>();
        for (Asiento a : asientos) {
            Optional<List<Extra>> extras = asientosReserva.extrasPara(a, pack, precios);
            String estado;
            if (propios.contains(a.getId())) {
                estado = SELECCIONADO;
            } else if (a.getClaseCabina() != cabina || extras.isEmpty() || tomados.contains(a.getId())) {
                estado = NO_DISPONIBLE;
            } else {
                estado = DISPONIBLE;
            }
            porFila.computeIfAbsent(a.getFila(), f -> new ArrayList<>()).add(new MapaAsientosResponse.Asiento(
                    AsientosReserva.codigo(a),
                    a.getLetra(),
                    AsientosReserva.categoria(a),
                    estado,
                    extras.map(AsientosReserva::precio).orElse(0L),
                    AsientosReserva.incluidoEnPack(a, pack)));
            cabinaPorFila.putIfAbsent(a.getFila(), a.getClaseCabina());
        }

        List<String> columnas = AsientosReserva.columnas(asientos);
        List<MapaAsientosResponse.Fila> filas = porFila.entrySet().stream()
                .map(e -> new MapaAsientosResponse.Fila(e.getKey(), cabinaPorFila.get(e.getKey()), e.getValue()))
                .toList();
        return new MapaAsientosResponse(vuelo.getId(), cabina, columnas, AsientosReserva.pasillosDespuesDe(columnas), filas);
    }

    // ---------------------------------------------------------------- endpoint 6

    /**
     * Aplica las selecciones sobre el estado actual y valida el resultado completo de cada tramo:
     * el asiento del adulto responsable debe existir antes que el del menor, y el adulto con sus menores
     * sentados deben quedar juntos en la misma fila. Los vuelos se bloquean mientras dura la operación.
     * El bloqueo de los asientos dura hasta expiraEn de la reserva (no se extiende).
     */
    @Override
    @Transactional
    public AsientosAsignadosResponse seleccionar(Long reservaId, String compraToken, SeleccionarAsientosRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        if (boletos.isEmpty()) {
            throw new ReglaNegocioException("Primero debes registrar los pasajeros de la reserva").conCodigo(DATOS_INCOMPLETOS);
        }
        Map<String, Boleto> boletoPorClave = new HashMap<>();
        boletos.forEach(b -> boletoPorClave.put(clave(b.getPasajeroId().getId(), b.getVueloId().getId()), b));

        // Bloquear los vuelos tocados, en orden de id, antes de leer qué asientos están tomados.
        Set<Long> vuelosTocados = new TreeSet<>();
        for (SeleccionarAsientosRequest.Seleccion s : request.selecciones()) {
            if (!boletoPorClave.containsKey(clave(s.pasajeroId(), s.tramoId()))) {
                throw new RecursoNoEncontradoException("El pasajero " + s.pasajeroId()
                        + " no tiene un boleto en el tramo " + s.tramoId() + " de esta reserva");
            }
            vuelosTocados.add(s.tramoId());
        }
        vuelosTocados.forEach(catalogo::bloquearVuelo);

        Map<TipoExtra, Extra> precios = asientosReserva.preciosVigentes();
        Map<Long, List<Asiento>> asientosPorVuelo = new HashMap<>();
        Map<Boleto, Asiento> estadoFinal = new LinkedHashMap<>();
        boletos.forEach(b -> estadoFinal.put(b, b.getAsientoId()));
        Map<Boleto, List<Extra>> cobros = new LinkedHashMap<>();

        for (SeleccionarAsientosRequest.Seleccion s : request.selecciones()) {
            Boleto boleto = boletoPorClave.get(clave(s.pasajeroId(), s.tramoId()));
            if (s.asiento() == null) {
                estadoFinal.put(boleto, null);
                cobros.put(boleto, List.of());
                continue;
            }
            if (boleto.getPasajeroId().getTipo() == TipoPasajero.BEBE) {
                throw new ReglaNegocioException("Los bebés viajan en brazos de su adulto y no ocupan asiento")
                        .conCodigo(BEBE_NO_OCUPA_ASIENTO);
            }
            List<Asiento> deLaAeronave = asientosPorVuelo.computeIfAbsent(boleto.getVueloId().getId(),
                    id -> catalogo.asientosDeAeronave(boleto.getVueloId().getAeronave().getId()));
            String codigo = s.asiento().trim().toUpperCase(Locale.ROOT);
            Asiento asiento = deLaAeronave.stream()
                    .filter(a -> AsientosReserva.codigo(a).equals(codigo))
                    .findFirst()
                    .orElseThrow(() -> noDisponible(codigo, boleto, "no existe en la aeronave"));
            ClaseCabina cabina = boleto.getTarifaVueloId().getClaseCabina();
            if (asiento.getClaseCabina() != cabina) {
                throw noDisponible(codigo, boleto, "no pertenece a la cabina " + cabina);
            }
            List<Extra> extras = asientosReserva.extrasPara(asiento, boleto.getTarifaVueloId().getPack(), precios)
                    .orElseThrow(() -> noDisponible(codigo, boleto, "no tiene precio configurado"));
            estadoFinal.put(boleto, asiento);
            cobros.put(boleto, extras);
        }

        exigirAsientosLibres(reserva, estadoFinal, cobros.keySet());
        exigirMenoresJuntoASuAdulto(estadoFinal, boletoPorClave, asientosPorVuelo);

        cobros.forEach((boleto, extras) -> asientosReserva.asignar(boleto, estadoFinal.get(boleto), extras));

        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, reserva.getPromocion(), false);
        calculadora.aplicar(reserva, total);
        reservaRepository.save(reserva);

        return new AsientosAsignadosResponse(
                ResumenReserva.asignaciones(boletos),
                reserva.getExpiraEn() != null ? reserva.getExpiraEn().atOffset(ZoneOffset.UTC) : null,
                total.aResponse());
    }

    /** Un asiento que cambia no puede estar tomado por otra compra ni repetirse dentro de esta reserva. */
    private void exigirAsientosLibres(Reserva reserva, Map<Boleto, Asiento> estadoFinal, Set<Boleto> cambiados) {
        Map<Long, Set<Long>> tomadosPorVuelo = new HashMap<>();
        Map<String, Boleto> usadosEnReserva = new HashMap<>();
        for (Map.Entry<Boleto, Asiento> e : estadoFinal.entrySet()) {
            Asiento asiento = e.getValue();
            if (asiento == null) {
                continue;
            }
            Boleto boleto = e.getKey();
            Long vueloId = boleto.getVueloId().getId();
            Boleto otro = usadosEnReserva.putIfAbsent(vueloId + ":" + asiento.getId(), boleto);
            if (otro != null) {
                throw noDisponible(AsientosReserva.codigo(asiento), boleto, "ya está asignado a otro pasajero de esta reserva");
            }
            if (cambiados.contains(boleto)) {
                Set<Long> tomados = tomadosPorVuelo.computeIfAbsent(vueloId,
                        id -> new HashSet<>(asientosTomadosPorOtras(id, reserva.getId())));
                if (tomados.contains(asiento.getId())) {
                    throw noDisponible(AsientosReserva.codigo(asiento), boleto, "ya fue tomado por otra compra");
                }
            }
        }
    }

    /** RF-05: el menor elige después que su adulto responsable y queda junto a él en la misma fila. */
    private void exigirMenoresJuntoASuAdulto(Map<Boleto, Asiento> estadoFinal, Map<String, Boleto> boletoPorClave,
                                            Map<Long, List<Asiento>> asientosPorVuelo) {
        Map<Boleto, List<Asiento>> grupos = new LinkedHashMap<>();
        for (Map.Entry<Boleto, Asiento> e : estadoFinal.entrySet()) {
            Boleto boletoMenor = e.getKey();
            Pasajero menor = boletoMenor.getPasajeroId();
            if (e.getValue() == null || menor.getTipo() != TipoPasajero.NINO || menor.getAdultoResponsable() == null) {
                continue;
            }
            Boleto boletoAdulto = boletoPorClave.get(clave(menor.getAdultoResponsable().getId(), boletoMenor.getVueloId().getId()));
            Asiento asientoAdulto = boletoAdulto != null ? estadoFinal.get(boletoAdulto) : null;
            if (asientoAdulto == null) {
                throw new ReglaNegocioException("Primero elige el asiento del adulto responsable de "
                        + menor.getNombre() + " en el vuelo " + boletoMenor.getVueloId().getCodigoVuelo())
                        .conCodigo(ADULTO_SIN_ASIENTO);
            }
            grupos.computeIfAbsent(boletoAdulto, b -> new ArrayList<>(List.of(asientoAdulto))).add(e.getValue());
        }
        for (Map.Entry<Boleto, List<Asiento>> g : grupos.entrySet()) {
            Boleto boletoAdulto = g.getKey();
            List<Asiento> deLaAeronave = asientosPorVuelo.computeIfAbsent(boletoAdulto.getVueloId().getId(),
                    id -> catalogo.asientosDeAeronave(boletoAdulto.getVueloId().getAeronave().getId()));
            if (!AsientosReserva.contiguos(g.getValue(), deLaAeronave)) {
                throw new ReglaNegocioException("Los menores a cargo de " + boletoAdulto.getPasajeroId().getNombre()
                        + " deben quedar junto a él, en la misma fila y sin pasillo entre medio (vuelo "
                        + boletoAdulto.getVueloId().getCodigoVuelo() + ")")
                        .conCodigo(MENOR_SEPARADO_DE_ADULTO);
            }
        }
    }

    private List<Long> asientosTomadosPorOtras(Long vueloId, Long reservaId) {
        LocalDateTime ahora = reservaAcceso.ahora();
        return boletoRepository.asientosTomadosPorOtras(vueloId, reservaId, EstadoReserva.CONFIRMADA, ReservaAcceso.ABIERTAS, ahora);
    }

    private static ConflictoException noDisponible(String codigo, Boleto boleto, String motivo) {
        return new ConflictoException("El asiento " + codigo + " del vuelo " + boleto.getVueloId().getCodigoVuelo()
                + " " + motivo).conCodigo(ASIENTO_NO_DISPONIBLE);
    }

    private static String clave(Long pasajeroId, Long vueloId) {
        return pasajeroId + ":" + vueloId;
    }
}
