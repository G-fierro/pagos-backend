package com.banduairlines.service.reserva;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.banduairlines.dto.reserva.AsignacionAsiento;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PromocionAplicadaResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.ServiciosContratadosResponse;
import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.TipoExtra;

/** Arma la vista de una reserva (GET /reservas/{id}) y sus totales a partir de las entidades de RESERVA. */
@Component
public class ResumenReserva {

    private final CalculadoraTotalReserva calculadora;

    public ResumenReserva(CalculadoraTotalReserva calculadora) {
        this.calculadora = calculadora;
    }

    public ReservaResponse armar(Reserva reserva, List<TramoReserva> tramos, List<Pasajero> pasajeros, List<Boleto> boletos) {
        String pack = tramos.isEmpty() ? null : tramos.get(0).getTarifaVuelo().getPack().getNombre();
        Promocion promocion = reserva.getPromocion();
        return new ReservaResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEstado(),
                reserva.getExpiraEn() != null ? reserva.getExpiraEn().atOffset(ZoneOffset.UTC) : null,
                pack,
                new ReservaResponse.CantidadPasajeros(
                        reserva.getCantidadAdultos(), reserva.getCantidadNinos(), reserva.getCantidadBebes()),
                tramos.stream().map(ResumenReserva::tramo).toList(),
                pasajeros.stream().map(p -> pasajero(p, null)).toList(),
                asignaciones(boletos),
                servicios(boletos),
                promocion == null ? null : new PromocionAplicadaResponse.Promocion(
                        promocion.getCodigo(),
                        promocion.getTipoDescuento().name(),
                        CalculadoraTotalReserva.clp(promocion.getValor()),
                        CalculadoraTotalReserva.clp(reserva.getDescuento()),
                        promocion.getVigenteHasta().atOffset(ZoneOffset.UTC)),
                reserva.getEmailContacto() == null && reserva.getNombreContacto() == null ? null
                        : new ContactoResponse(reserva.getNombreContacto(), reserva.getEmailContacto(), reserva.getTelefonoContacto()),
                total(reserva, tramos, boletos));
    }

    /**
     * Con boletos usa la calculadora (precios guardados en la reserva). Antes de registrar pasajeros
     * solo existen los tramos: tarifa de cada tramo por la cantidad declarada de pasajeros.
     */
    public TotalResponse total(Reserva reserva, List<TramoReserva> tramos, List<Boleto> boletos) {
        if (!boletos.isEmpty()) {
            return calculadora.calcular(boletos, reserva.getPromocion(), false).aResponse();
        }
        int pasajeros = valor(reserva.getCantidadAdultos()) + valor(reserva.getCantidadNinos()) + valor(reserva.getCantidadBebes());
        long porPasajero = tramos.stream().mapToLong(t -> CalculadoraTotalReserva.clp(t.getPrecioTarifa())).sum();
        return totalInicial(pasajeros * tramos.size(), porPasajero * pasajeros);
    }

    /** Mismo formato que CalculadoraTotalReserva: una línea TARIFA con la cantidad de boletos. */
    public static TotalResponse totalInicial(int boletos, long total) {
        String detalle = boletos == 1 ? "Tarifa (1 boleto)" : "Tarifas (" + boletos + " boletos)";
        return new TotalResponse(
                List.of(new TotalResponse.Linea("TARIFA", detalle, total)),
                0,
                total,
                CalculadoraTotalReserva.MONEDA);
    }

    public static PasajeroResponse pasajero(Pasajero p, String ref) {
        return new PasajeroResponse(
                p.getId(),
                ref,
                p.getTipo(),
                p.getNombre(),
                p.getApellido(),
                p.getFechaNacimiento(),
                p.getTipoDocumento(),
                p.getNumeroDocumento(),
                p.getNacionalidad(),
                p.getAdultoResponsable() != null ? p.getAdultoResponsable().getId() : null);
    }

    public static List<AsignacionAsiento> asignaciones(List<Boleto> boletos) {
        return boletos.stream()
                .map(b -> new AsignacionAsiento(
                        b.getPasajeroId().getId(),
                        b.getVueloId().getId(),
                        b.getAsientoId() != null ? AsientosReserva.codigo(b.getAsientoId()) : null,
                        precioAsiento(b)))
                .toList();
    }

    /** Lo cobrado por el asiento del boleto (extra ASIENTO_PREFERENTE). */
    public static long precioAsiento(Boleto boleto) {
        long total = 0;
        for (BoletoExtra be : boleto.getExtras()) {
            TipoExtra tipo = be.getExtra().getTipo();
            if (tipo == TipoExtra.ASIENTO_PREFERENTE) {
                total += CalculadoraTotalReserva.clp(be.getPrecioUnitario()) * be.getCantidad();
            }
        }
        return total;
    }

    /** Mismo formato que la respuesta de PUT /servicios. */
    public static List<ServiciosContratadosResponse.Servicio> servicios(List<Boleto> boletos) {
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

    private static ReservaResponse.Tramo tramo(TramoReserva t) {
        Vuelo v = t.getVuelo();
        return new ReservaResponse.Tramo(
                v.getId(),
                t.getOrden(),
                t.getSentido(),
                v.getCodigoVuelo(),
                v.getRuta() != null ? v.getRuta().getOrigen().getCodigoIata() : null,
                v.getRuta() != null ? v.getRuta().getDestino().getCodigoIata() : null,
                v.getSalidaProgramada(),
                v.getLlegadaProgramada(),
                t.getTarifaVuelo().getClaseCabina(),
                v.getEstado());
    }

    private static int valor(Integer n) {
        return n == null ? 0 : n;
    }
}
