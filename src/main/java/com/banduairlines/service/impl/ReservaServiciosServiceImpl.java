package com.banduairlines.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.CatalogoServiciosResponse;
import com.banduairlines.dto.reserva.ContratarServiciosRequest;
import com.banduairlines.dto.reserva.ServiciosContratadosResponse;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaServiciosService;

@Service
@Transactional
public class ReservaServiciosServiceImpl implements ReservaServiciosService {

    static final String SERVICIO_NO_PERMITIDO = "SERVICIO_NO_PERMITIDO";
    static final String DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS";

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;
    private final ExtraRepository extraRepository;
    private final CalculadoraTotalReserva calculadora;

    public ReservaServiciosServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                       BoletoRepository boletoRepository, ExtraRepository extraRepository,
                                       CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
        this.extraRepository = extraRepository;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoServiciosResponse obtenerCatalogo(Long reservaId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        List<Boleto> boletos = exigirBoletos(reserva);
        PackTarifario pack = boletos.get(0).getTarifaVueloId().getPack();

        List<CatalogoServiciosResponse.Incluido> incluidos = new ArrayList<>();
        incluidos.add(new CatalogoServiciosResponse.Incluido("ARTICULO_PERSONAL", 1, "Bolso o mochila bajo el asiento"));
        if (Boolean.TRUE.equals(pack.getIncluyeEquipajeCabina())) {
            incluidos.add(new CatalogoServiciosResponse.Incluido("EQUIPAJE_CABINA", 1, "Maleta de cabina"));
        }
        if (pack.getPiezasBodegaIncluidas() != null && pack.getPiezasBodegaIncluidas() > 0) {
            incluidos.add(new CatalogoServiciosResponse.Incluido("EQUIPAJE_BODEGA", pack.getPiezasBodegaIncluidas(),
                    "Maleta en bodega de " + pack.getKgPorPiezaBodega() + " kg"));
        }

        List<CatalogoServiciosResponse.Disponible> disponibles = extraRepository.findByActivoTrueOrderByIdAsc().stream()
                .filter(extra -> permitidoEnPack(extra, pack))
                .map(extra -> new CatalogoServiciosResponse.Disponible(
                        String.valueOf(extra.getId()),
                        extra.getNombre(),
                        categoria(extra.getTipo()),
                        CalculadoraTotalReserva.clp(extra.getPrecio()),
                        true,
                        maximoPorPasajero(extra.getTipo())))
                .toList();

        return new CatalogoServiciosResponse(incluidos, disponibles);
    }

    @Override
    public ServiciosContratadosResponse contratar(Long reservaId, String compraToken, ContratarServiciosRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);
        List<Boleto> boletos = exigirBoletos(reserva);

        Map<String, Boleto> boletoPorTramo = new HashMap<>();
        for (Boleto b : boletos) {
            boletoPorTramo.put(clave(b.getPasajeroId().getId(), b.getVueloId().getId()), b);
        }
        Map<Long, Extra> catalogo = extraRepository.findByActivoTrueOrderByIdAsc().stream()
                .collect(Collectors.toMap(Extra::getId, Function.identity()));

        // Cantidad pedida por boleto y servicio (si el mismo servicio viene repetido se suma).
        Map<Boleto, Map<Long, Integer>> pedido = new LinkedHashMap<>();
        for (ContratarServiciosRequest.Servicio item : request.servicios()) {
            Extra extra = buscarServicio(catalogo, item.servicioId());
            Boleto boleto = boletoPorTramo.get(clave(item.pasajeroId(), item.tramoId()));
            if (boleto == null) {
                throw new ReglaNegocioException("El pasajero " + item.pasajeroId()
                        + " no tiene un boleto en el tramo " + item.tramoId() + " de esta reserva")
                        .conCodigo(SERVICIO_NO_PERMITIDO);
            }
            PackTarifario pack = boleto.getTarifaVueloId().getPack();
            if (!permitidoEnPack(extra, pack)) {
                throw new ReglaNegocioException("El servicio " + extra.getNombre()
                        + " no aplica a la tarifa " + pack.getNombre())
                        .conCodigo(SERVICIO_NO_PERMITIDO);
            }
            pedido.computeIfAbsent(boleto, b -> new LinkedHashMap<>()).merge(extra.getId(), item.cantidad(), Integer::sum);
        }
        pedido.forEach((boleto, servicios) -> servicios.forEach((extraId, cantidad) -> {
            Extra extra = catalogo.get(extraId);
            int maximo = maximoPorPasajero(extra.getTipo());
            if (cantidad > maximo) {
                throw new ReglaNegocioException("Se puede contratar como máximo " + maximo + " de "
                        + extra.getNombre() + " por pasajero y tramo")
                        .conCodigo(SERVICIO_NO_PERMITIDO);
            }
        }));

        for (Boleto boleto : boletos) {
            reemplazarServicios(boleto, pedido.getOrDefault(boleto, Map.of()), catalogo);
        }

        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, reserva.getPromocion(), false);
        calculadora.aplicar(reserva, total);
        reservaRepository.save(reserva);

        return new ServiciosContratadosResponse(serviciosContratados(boletos), total.aResponse());
    }

    /**
     * Deja en el boleto exactamente los servicios pedidos. Los existentes se actualizan en el mismo
     * registro (Hibernate inserta antes de borrar, así que borrar y volver a crear la misma clave
     * compuesta fallaría). Los asientos preferentes son del endpoint de asientos y no se tocan.
     */
    private void reemplazarServicios(Boleto boleto, Map<Long, Integer> pedidos, Map<Long, Extra> catalogo) {
        Map<Long, Integer> pendientes = new LinkedHashMap<>(pedidos);
        Iterator<BoletoExtra> it = boleto.getExtras().iterator();
        while (it.hasNext()) {
            BoletoExtra actual = it.next();
            if (!CalculadoraTotalReserva.esServicio(actual.getExtra().getTipo())) {
                continue;
            }
            Integer cantidad = pendientes.remove(actual.getExtra().getId());
            if (cantidad == null) {
                it.remove();
            } else {
                actual.setCantidad(cantidad);
                actual.setPrecioUnitario(actual.getExtra().getPrecio());
            }
        }
        pendientes.forEach((extraId, cantidad) -> {
            Extra extra = catalogo.get(extraId);
            boleto.addExtra(extra, cantidad, extra.getPrecio());
        });
    }

    private List<ServiciosContratadosResponse.Servicio> serviciosContratados(List<Boleto> boletos) {
        List<ServiciosContratadosResponse.Servicio> servicios = new ArrayList<>();
        for (Boleto b : boletos) {
            for (BoletoExtra be : b.getExtras()) {
                if (CalculadoraTotalReserva.esServicio(be.getExtra().getTipo())) {
                    servicios.add(new ServiciosContratadosResponse.Servicio(
                            String.valueOf(be.getExtra().getId()),
                            b.getPasajeroId().getId(),
                            b.getVueloId().getId(),
                            be.getCantidad(),
                            be.getExtra().getNombre(),
                            CalculadoraTotalReserva.clp(be.getPrecioUnitario()) * be.getCantidad()));
                }
            }
        }
        return servicios;
    }

    private List<Boleto> exigirBoletos(Reserva reserva) {
        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        if (boletos.isEmpty()) {
            throw new ReglaNegocioException("Primero debes registrar los pasajeros de la reserva")
                    .conCodigo(DATOS_INCOMPLETOS);
        }
        return boletos;
    }

    private Extra buscarServicio(Map<Long, Extra> catalogo, String servicioId) {
        Extra extra = null;
        try {
            extra = catalogo.get(Long.valueOf(servicioId.trim()));
        } catch (NumberFormatException e) {
            // un servicioId no numérico simplemente no existe en el catálogo
        }
        if (extra == null || !CalculadoraTotalReserva.esServicio(extra.getTipo())) {
            throw new ReglaNegocioException("El servicio " + servicioId + " no existe o no está disponible")
                    .conCodigo(SERVICIO_NO_PERMITIDO);
        }
        return extra;
    }

    /** No se ofrece lo que la tarifa ya incluye; el asiento preferente se cobra en el endpoint de asientos. */
    static boolean permitidoEnPack(Extra extra, PackTarifario pack) {
        return switch (extra.getTipo()) {
            case ASIENTO_PREFERENTE -> false;
            case EQUIPAJE_CABINA -> !Boolean.TRUE.equals(pack.getIncluyeEquipajeCabina());
            case EMBARQUE_PRIORITARIO -> !Boolean.TRUE.equals(pack.getIncluyeEmbarquePrioritario());
            case EQUIPAJE_BODEGA -> true;
        };
    }

    static int maximoPorPasajero(TipoExtra tipo) {
        return tipo == TipoExtra.EQUIPAJE_BODEGA ? 3 : 1;
    }

    private static String categoria(TipoExtra tipo) {
        return switch (tipo) {
            case EQUIPAJE_CABINA, EQUIPAJE_BODEGA -> "EQUIPAJE";
            case EMBARQUE_PRIORITARIO -> "EMBARQUE_PRIORITARIO";
            case ASIENTO_PREFERENTE -> "OTRO";
        };
    }

    private static String clave(Long pasajeroId, Long vueloId) {
        return Objects.toString(pasajeroId) + ":" + Objects.toString(vueloId);
    }
}
