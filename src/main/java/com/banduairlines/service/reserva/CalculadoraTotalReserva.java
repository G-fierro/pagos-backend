package com.banduairlines.service.reserva;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.model.enums.TipoExtra;

/**
 * Calcula el total de una reserva a partir de sus boletos (tarifa) y de los extras de cada boleto
 * (asiento preferente, equipaje y embarque prioritario), y le aplica el descuento de la promoción.
 * Todos los montos son CLP enteros.
 */
@Component
public class CalculadoraTotalReserva {

    public static final String MONEDA = "CLP";

    public record Resultado(long subtotal, long descuento, long total, List<TotalResponse.Linea> lineas) {

        public TotalResponse aResponse() {
            return new TotalResponse(lineas, descuento, total, MONEDA);
        }
    }

    /**
     * @param preciosVigentes false usa los precios guardados en la reserva (lo que vio el pasajero);
     *                        true usa los precios actuales de la tarifa y del catálogo de extras.
     */
    public Resultado calcular(List<Boleto> boletos, Promocion promocion, boolean preciosVigentes) {
        long tarifas = 0;
        long asientos = 0;
        long equipaje = 0;
        long servicios = 0;

        for (Boleto boleto : boletos) {
            tarifas += clp(preciosVigentes ? boleto.getTarifaVueloId().getPrecio() : boleto.getPrecioTarifa());
            for (BoletoExtra be : boleto.getExtras()) {
                BigDecimal unitario = preciosVigentes ? be.getExtra().getPrecio() : be.getPrecioUnitario();
                long monto = clp(unitario) * be.getCantidad();
                switch (be.getExtra().getTipo()) {
                    case ASIENTO_PREFERENTE -> asientos += monto;
                    case EQUIPAJE_CABINA, EQUIPAJE_BODEGA -> equipaje += monto;
                    case EMBARQUE_PRIORITARIO -> servicios += monto;
                }
            }
        }

        long subtotal = tarifas + asientos + equipaje + servicios;
        long descuento = promocion != null ? descuento(promocion, subtotal) : 0;

        List<TotalResponse.Linea> lineas = new ArrayList<>();
        String detalleTarifas = boletos.size() == 1 ? "Tarifa (1 boleto)" : "Tarifas (" + boletos.size() + " boletos)";
        lineas.add(new TotalResponse.Linea("TARIFA", detalleTarifas, tarifas));
        if (asientos > 0) {
            lineas.add(new TotalResponse.Linea("ASIENTOS", "Selección de asientos", asientos));
        }
        if (equipaje > 0) {
            lineas.add(new TotalResponse.Linea("EQUIPAJE", "Equipaje adicional", equipaje));
        }
        if (servicios > 0) {
            lineas.add(new TotalResponse.Linea("SERVICIOS", "Embarque prioritario", servicios));
        }
        if (descuento > 0) {
            lineas.add(new TotalResponse.Linea("DESCUENTO", "Código " + promocion.getCodigo(), -descuento));
        }
        return new Resultado(subtotal, descuento, subtotal - descuento, lineas);
    }

    /** PORCENTAJE se trunca al peso; MONTO_FIJO nunca supera el subtotal. */
    public long descuento(Promocion promocion, long subtotal) {
        long valor = clp(promocion.getValor());
        long descuento = promocion.getTipoDescuento() == TipoDescuento.PORCENTAJE
                ? BigDecimal.valueOf(subtotal).multiply(promocion.getValor())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).longValue()
                : valor;
        return Math.max(0, Math.min(descuento, subtotal));
    }

    public boolean promocionVigente(Promocion promocion, LocalDateTime ahora) {
        return Boolean.TRUE.equals(promocion.getActiva())
                && !ahora.isBefore(promocion.getVigenteDesde())
                && !ahora.isAfter(promocion.getVigenteHasta())
                && (promocion.getUsosMaximos() == null || promocion.getUsosActuales() < promocion.getUsosMaximos());
    }

    /** Guarda subtotal, descuento y total en la cabecera de la reserva. */
    public void aplicar(Reserva reserva, Resultado resultado) {
        reserva.setSubtotal(BigDecimal.valueOf(resultado.subtotal()));
        reserva.setDescuento(BigDecimal.valueOf(resultado.descuento()));
        reserva.setTotal(BigDecimal.valueOf(resultado.total()));
    }

    public static long clp(BigDecimal monto) {
        return monto == null ? 0 : monto.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static boolean esServicio(TipoExtra tipo) {
        return tipo != TipoExtra.ASIENTO_PREFERENTE;
    }
}
