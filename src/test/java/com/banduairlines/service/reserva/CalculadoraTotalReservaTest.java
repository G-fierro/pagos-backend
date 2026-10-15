package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.model.enums.TipoExtra;

class CalculadoraTotalReservaTest {

    private final CalculadoraTotalReserva calculadora = new CalculadoraTotalReserva();

    @Test
    void calcular_sumaTarifasYExtrasPorConcepto() {
        Reserva reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        Boleto b1 = ReservaFixtures.boleto(reserva, 1, 10, 59990, ReservaFixtures.packLight());
        Boleto b2 = ReservaFixtures.boleto(reserva, 2, 10, 59990, ReservaFixtures.packLight());
        b1.addExtra(ReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 24990), 2, BigDecimal.valueOf(24990));
        b2.addExtra(ReservaFixtures.extra(3, TipoExtra.EMBARQUE_PRIORITARIO, 4990), 1, BigDecimal.valueOf(4990));
        b2.addExtra(ReservaFixtures.extra(4, TipoExtra.ASIENTO_PREFERENTE, 6500), 1, BigDecimal.valueOf(6500));

        CalculadoraTotalReserva.Resultado r = calculadora.calcular(List.of(b1, b2), null, false);

        assertThat(r.subtotal()).isEqualTo(119980 + 49980 + 4990 + 6500);
        assertThat(r.descuento()).isZero();
        assertThat(r.total()).isEqualTo(r.subtotal());
        assertThat(r.lineas()).extracting("concepto").containsExactly("TARIFA", "ASIENTOS", "EQUIPAJE", "SERVICIOS");
        assertThat(r.lineas().get(0).descripcion()).isEqualTo("Tarifas (2 boletos)");
    }

    @Test
    void calcular_conPorcentaje_truncaAlPesoYAgregaLineaNegativa() {
        Reserva reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        Boleto b = ReservaFixtures.boleto(reserva, 1, 10, 187985, ReservaFixtures.packLight());

        CalculadoraTotalReserva.Resultado r = calculadora.calcular(List.of(b),
                ReservaFixtures.promocion("BANDU10", TipoDescuento.PORCENTAJE, 10), false);

        assertThat(r.descuento()).isEqualTo(18798);
        assertThat(r.total()).isEqualTo(187985 - 18798);
        assertThat(r.lineas().get(r.lineas().size() - 1).montoCLP()).isEqualTo(-18798);
        assertThat(r.lineas().get(0).descripcion()).isEqualTo("Tarifa (1 boleto)");
    }

    @Test
    void descuento_montoFijo_noSuperaElSubtotal() {
        assertThat(calculadora.descuento(ReservaFixtures.promocion("FIJO", TipoDescuento.MONTO_FIJO, 50000), 30000))
                .isEqualTo(30000);
    }

    @Test
    void calcular_preciosVigentes_usaLaTarifaYElCatalogoActuales() {
        Reserva reserva = ReservaFixtures.reserva(EstadoReserva.EN_CURSO);
        Boleto b = ReservaFixtures.boleto(reserva, 1, 10, 50000, ReservaFixtures.packLight());
        b.getTarifaVueloId().setPrecio(BigDecimal.valueOf(55000));
        b.addExtra(ReservaFixtures.extra(1, TipoExtra.EQUIPAJE_BODEGA, 26000), 1, BigDecimal.valueOf(24990));

        assertThat(calculadora.calcular(List.of(b), null, false).total()).isEqualTo(50000 + 24990);
        assertThat(calculadora.calcular(List.of(b), null, true).total()).isEqualTo(55000 + 26000);
    }
}
