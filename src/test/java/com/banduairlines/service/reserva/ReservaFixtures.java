package com.banduairlines.service.reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.model.enums.TipoExtra;

/** Datos de prueba compartidos por los tests de RESERVA (endpoints 7 al 12). */
final class ReservaFixtures {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 1, 18, 0);

    private ReservaFixtures() {
    }

    static PackTarifario packLight() {
        return PackTarifario.builder().id(1L).nombre("Light")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(0).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(false).incluyeEmbarquePrioritario(false).permiteCambios(false)
                .build();
    }

    static Reserva reserva(EstadoReserva estado) {
        return Reserva.builder().id(1L).codigoReserva("TMP001").emailContacto("tomas@correo.cl")
                .estado(estado).subtotal(BigDecimal.ZERO).descuento(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .expiraEn(AHORA.plusMinutes(10))
                .build();
    }

    static Boleto boleto(Reserva reserva, long pasajeroId, long vueloId, long precioTarifa, PackTarifario pack) {
        Vuelo vuelo = Vuelo.builder().id(vueloId).codigoVuelo("BA-" + vueloId).estado(EstadoVuelo.PROGRAMADO).build();
        TarifaVuelo tarifa = TarifaVuelo.builder().id(vueloId).vuelo(vuelo).pack(pack)
                .precio(BigDecimal.valueOf(precioTarifa)).build();
        return Boleto.builder().id(pasajeroId * 100 + vueloId).reserva(reserva)
                .pasajeroId(Pasajero.builder().id(pasajeroId).build())
                .vueloId(vuelo).tarifaVueloId(tarifa)
                .precioTarifa(BigDecimal.valueOf(precioTarifa))
                .build();
    }

    static Extra extra(long id, TipoExtra tipo, long precio) {
        return Extra.builder().id(id).tipo(tipo).nombre(tipo.name()).precio(BigDecimal.valueOf(precio)).activo(true).build();
    }

    static Promocion promocion(String codigo, TipoDescuento tipo, long valor) {
        return Promocion.builder().id(1L).codigo(codigo).tipoDescuento(tipo).valor(BigDecimal.valueOf(valor))
                .vigenteDesde(AHORA.minusDays(30)).vigenteHasta(AHORA.plusDays(30))
                .usosActuales(0).activa(true)
                .build();
    }
}
