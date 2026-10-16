package com.banduairlines.service.impl;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.reserva.AsientosReserva;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaPasajerosService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaPasajerosServiceImpl implements ReservaPasajerosService {

    static final String CANTIDAD_PASAJEROS_DISTINTA = "CANTIDAD_PASAJEROS_DISTINTA";
    static final String TIPO_NO_COINCIDE_EDAD = "TIPO_NO_COINCIDE_EDAD";
    static final String MENOR_SIN_ADULTO_RESPONSABLE = "MENOR_SIN_ADULTO_RESPONSABLE";
    static final String DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS";

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final TramoReservaRepository tramoReservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final AsientosReserva asientosReserva;
    private final CalculadoraTotalReserva calculadora;

    public ReservaPasajerosServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                       TramoReservaRepository tramoReservaRepository, PasajeroRepository pasajeroRepository,
                                       BoletoRepository boletoRepository, AsientosReserva asientosReserva,
                                       CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.tramoReservaRepository = tramoReservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.asientosReserva = asientosReserva;
        this.calculadora = calculadora;
    }

    /**
     * Primera vez: crea los pasajeros y un Boleto por pasajero y tramo.
     * Siguientes veces: actualiza los mismos registros (emparejados por orden dentro de cada tipo), así
     * se conservan los asientos y servicios ya elegidos. Si cambia el adulto responsable de un menor,
     * se liberan los asientos del menor para que vuelva a elegirlos junto a su nuevo adulto.
     */
    @Override
    @Transactional
    public PasajerosRegistradosResponse registrar(Long reservaId, String compraToken, RegistrarPasajerosRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        List<TramoReserva> tramos = tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(reserva.getId());
        if (tramos.isEmpty()) {
            throw new ReglaNegocioException("La reserva no tiene tramos registrados").conCodigo(DATOS_INCOMPLETOS);
        }
        List<RegistrarPasajerosRequest.Pasajero> pedidos = request.pasajeros();

        exigirCantidades(reserva, pedidos);
        exigirTiposSegunEdad(pedidos, fechaPrimerTramo(tramos));
        Map<String, String> responsablePorRef = exigirAdultosResponsables(reserva, pedidos);

        // Emparejar cada pasajero pedido con un registro existente del mismo tipo (o uno nuevo).
        List<Pasajero> existentes = pasajeroRepository.findByReserva_IdOrderByIdAsc(reserva.getId());
        Map<TipoPasajero, List<Pasajero>> existentesPorTipo = new EnumMap<>(TipoPasajero.class);
        existentes.forEach(p -> existentesPorTipo.computeIfAbsent(p.getTipo(), t -> new ArrayList<>()).add(p));
        Map<Long, Long> responsableAnterior = new HashMap<>();
        existentes.forEach(p -> responsableAnterior.put(p.getId(),
                p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null));

        Map<String, Pasajero> porRef = new LinkedHashMap<>();
        Map<TipoPasajero, Integer> usados = new EnumMap<>(TipoPasajero.class);
        for (RegistrarPasajerosRequest.Pasajero pedido : pedidos) {
            int indice = usados.merge(pedido.tipo(), 1, Integer::sum) - 1;
            List<Pasajero> mismosTipo = existentesPorTipo.getOrDefault(pedido.tipo(), List.of());
            Pasajero pasajero = indice < mismosTipo.size()
                    ? mismosTipo.get(indice)
                    : Pasajero.builder().reserva(reserva).tipo(pedido.tipo()).build();
            pasajero.setNombre(pedido.nombres().trim());
            pasajero.setApellido(pedido.apellidos().trim());
            pasajero.setFechaNacimiento(pedido.fechaNacimiento());
            pasajero.setTipoDocumento(pedido.tipoDocumento());
            pasajero.setNumeroDocumento(pedido.numeroDocumento().trim());
            pasajero.setNacionalidad(pedido.nacionalidad().trim());
            pasajero.setAdultoResponsable(null);
            porRef.put(pedido.ref().trim(), pasajero);
        }
        List<Pasajero> guardados = pasajeroRepository.saveAll(porRef.values());

        // El adulto responsable se enlaza cuando todos tienen id.
        responsablePorRef.forEach((refMenor, refAdulto) -> porRef.get(refMenor).setAdultoResponsable(porRef.get(refAdulto)));
        pasajeroRepository.saveAll(guardados);

        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        if (boletos.isEmpty()) {
            boletos = boletoRepository.saveAll(crearBoletos(reserva, tramos, guardados));
        } else {
            liberarAsientosSiCambioElResponsable(boletos, responsableAnterior);
        }

        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, reserva.getPromocion(), false);
        calculadora.aplicar(reserva, total);
        reservaRepository.save(reserva);

        List<PasajeroResponse> respuesta = new ArrayList<>();
        porRef.forEach((ref, p) -> respuesta.add(ResumenReserva.pasajero(p, ref)));
        return new PasajerosRegistradosResponse(respuesta);
    }

    /** La cantidad y el tipo deben coincidir con lo declarado al crear la reserva. */
    private static void exigirCantidades(Reserva reserva, List<RegistrarPasajerosRequest.Pasajero> pedidos) {
        Map<TipoPasajero, Long> cantidad = new EnumMap<>(TipoPasajero.class);
        pedidos.forEach(p -> cantidad.merge(p.tipo(), 1L, Long::sum));
        long adultos = cantidad.getOrDefault(TipoPasajero.ADULTO, 0L);
        long ninos = cantidad.getOrDefault(TipoPasajero.NINO, 0L);
        long bebes = cantidad.getOrDefault(TipoPasajero.BEBE, 0L);
        if (adultos != valor(reserva.getCantidadAdultos()) || ninos != valor(reserva.getCantidadNinos())
                || bebes != valor(reserva.getCantidadBebes())) {
            throw new ReglaNegocioException("La reserva es para " + valor(reserva.getCantidadAdultos()) + " adultos, "
                    + valor(reserva.getCantidadNinos()) + " niños y " + valor(reserva.getCantidadBebes())
                    + " bebés; se enviaron " + adultos + ", " + ninos + " y " + bebes)
                    .conCodigo(CANTIDAD_PASAJEROS_DISTINTA);
        }
    }

    /** ADULTO desde 12 años, NINO de 2 a 11 y BEBE menor de 2, a la fecha de salida del primer tramo. */
    private static void exigirTiposSegunEdad(List<RegistrarPasajerosRequest.Pasajero> pedidos, LocalDate fechaVuelo) {
        for (RegistrarPasajerosRequest.Pasajero p : pedidos) {
            TipoPasajero segunEdad = tipoSegunEdad(p.fechaNacimiento(), fechaVuelo);
            if (segunEdad != p.tipo()) {
                throw new ReglaNegocioException("El pasajero " + p.ref() + " se registró como " + p.tipo()
                        + ", pero a la fecha del vuelo (" + fechaVuelo + ") corresponde a " + segunEdad)
                        .conCodigo(TIPO_NO_COINCIDE_EDAD);
            }
        }
    }

    public static TipoPasajero tipoSegunEdad(LocalDate fechaNacimiento, LocalDate fechaVuelo) {
        int edad = Period.between(fechaNacimiento, fechaVuelo).getYears();
        if (edad >= 12) {
            return TipoPasajero.ADULTO;
        }
        return edad >= 2 ? TipoPasajero.NINO : TipoPasajero.BEBE;
    }

    /**
     * Todo bebé, y todo niño que viaja con adultos en la reserva, debe indicar un ADULTO de la misma petición.
     * Un adulto puede ser responsable de varios niños, pero de un solo bebé (va en sus brazos).
     * En un ADULTO, adultoResponsableRef se ignora. Devuelve ref del menor → ref de su adulto.
     */
    private static Map<String, String> exigirAdultosResponsables(Reserva reserva, List<RegistrarPasajerosRequest.Pasajero> pedidos) {
        Map<String, RegistrarPasajerosRequest.Pasajero> porRef = new HashMap<>();
        pedidos.forEach(p -> porRef.put(p.ref().trim(), p));
        boolean hayAdultos = valor(reserva.getCantidadAdultos()) > 0;

        Map<String, String> responsables = new LinkedHashMap<>();
        Map<String, Integer> bebesPorAdulto = new HashMap<>();
        for (RegistrarPasajerosRequest.Pasajero p : pedidos) {
            if (p.tipo() == TipoPasajero.ADULTO) {
                continue;
            }
            String refAdulto = p.adultoResponsableRef() == null || p.adultoResponsableRef().isBlank()
                    ? null : p.adultoResponsableRef().trim();
            boolean obligatorio = p.tipo() == TipoPasajero.BEBE || hayAdultos;
            if (refAdulto == null) {
                if (obligatorio) {
                    throw new ReglaNegocioException("El pasajero " + p.ref() + " (" + p.tipo()
                            + ") debe indicar su adulto responsable")
                            .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
                }
                continue;
            }
            RegistrarPasajerosRequest.Pasajero adulto = porRef.get(refAdulto);
            if (adulto == null || adulto.tipo() != TipoPasajero.ADULTO) {
                throw new ReglaNegocioException("El adulto responsable " + refAdulto + " del pasajero " + p.ref()
                        + " no es un ADULTO de esta reserva")
                        .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
            }
            if (p.tipo() == TipoPasajero.BEBE && bebesPorAdulto.merge(refAdulto, 1, Integer::sum) > 1) {
                throw new ReglaNegocioException("El adulto " + refAdulto + " ya es responsable de otro bebé; cada bebé"
                        + " viaja en brazos de un adulto distinto")
                        .conCodigo(MENOR_SIN_ADULTO_RESPONSABLE);
            }
            responsables.put(p.ref().trim(), refAdulto);
        }
        return responsables;
    }

    private static List<Boleto> crearBoletos(Reserva reserva, List<TramoReserva> tramos, List<Pasajero> pasajeros) {
        List<Boleto> boletos = new ArrayList<>();
        for (TramoReserva tramo : tramos) {
            for (Pasajero pasajero : pasajeros) {
                boletos.add(Boleto.builder()
                        .reserva(reserva)
                        .pasajeroId(pasajero)
                        .vueloId(tramo.getVuelo())
                        .tarifaVueloId(tramo.getTarifaVuelo())
                        .sentido(tramo.getSentido())
                        .ordenTramo(tramo.getOrden())
                        .precioTarifa(tramo.getPrecioTarifa())
                        .build());
            }
        }
        return boletos;
    }

    private void liberarAsientosSiCambioElResponsable(List<Boleto> boletos, Map<Long, Long> responsableAnterior) {
        for (Boleto boleto : boletos) {
            Pasajero p = boleto.getPasajeroId();
            Long actual = p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null;
            if (p.getTipo() != TipoPasajero.ADULTO && boleto.getAsientoId() != null
                    && !Objects.equals(actual, responsableAnterior.get(p.getId()))) {
                asientosReserva.asignar(boleto, null, List.of());
            }
        }
    }

    private static LocalDate fechaPrimerTramo(List<TramoReserva> tramos) {
        return tramos.stream()
                .map(t -> t.getVuelo().getSalidaProgramada())
                .min(Comparator.naturalOrder())
                .orElseThrow()
                .toLocalDate();
    }

    private static long valor(Integer n) {
        return n == null ? 0 : n;
    }
}
