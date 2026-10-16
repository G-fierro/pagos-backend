package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.TipoExtra;

class AsientosReservaTest {

    private final List<Asiento> aeronave = aeronave();
    private final AsientosReserva asientos = new AsientosReserva(null);
    private final Extra preferente = ReservaFixtures.extra(51, TipoExtra.ASIENTO_PREFERENTE, 5000);
    private final Map<TipoExtra, Extra> precios = Map.of(TipoExtra.ASIENTO_PREFERENTE, preferente);

    @Test
    void pasillosDespuesDe_columnasPares_pasilloCentral() {
        assertThat(AsientosReserva.pasillosDespuesDe(List.of("A", "B", "C", "D", "E", "F"))).containsExactly("C");
        assertThat(AsientosReserva.pasillosDespuesDe(List.of("A", "C", "D", "F"))).containsExactly("C");
        assertThat(AsientosReserva.pasillosDespuesDe(List.of("A", "B", "C"))).isEmpty();
    }

    @Test
    void contiguos_vecinosEnLaMismaFilaSinPasillo() {
        assertThat(AsientosReserva.contiguos(List.of(a(5, "A"), a(5, "B"), a(5, "C")), aeronave)).isTrue();
        assertThat(AsientosReserva.contiguos(List.of(a(5, "C"), a(5, "D")), aeronave)).isFalse();
        assertThat(AsientosReserva.contiguos(List.of(a(5, "A"), a(5, "C")), aeronave)).isFalse();
        assertThat(AsientosReserva.contiguos(List.of(a(5, "A"), a(6, "A")), aeronave)).isFalse();
        // En BUSINESS la fila solo tiene A, C, D y F: A y C están uno al lado del otro.
        assertThat(AsientosReserva.contiguos(List.of(a(1, "A"), a(1, "C")), aeronave)).isTrue();
        assertThat(AsientosReserva.contiguos(List.of(a(1, "C"), a(1, "D")), aeronave)).isFalse();
    }

    @Test
    void extrasPara_asientoNormal_esGratisConCualquierPack() {
        assertThat(asientos.extrasPara(a(5, "A"), pack(false), precios)).contains(List.of());
        assertThat(asientos.extrasPara(a(5, "A"), pack(true), precios)).contains(List.of());
    }

    @Test
    void extrasPara_preferente_seCobraSoloSiElPackNoIncluyeSeleccion() {
        assertThat(asientos.extrasPara(a(10, "A"), pack(false), precios)).contains(List.of(preferente));
        assertThat(asientos.extrasPara(a(10, "A"), pack(true), precios)).contains(List.of());
        assertThat(AsientosReserva.incluidoEnPack(a(10, "A"), pack(false))).isFalse();
        assertThat(AsientosReserva.incluidoEnPack(a(10, "A"), pack(true))).isTrue();
    }

    @Test
    void extrasPara_business_conPackQueIncluyeSeleccion_noCobraAunqueSeaPreferente() {
        Asiento business = a(1, "A");
        assertThat(business.getEsPreferente()).isTrue();

        assertThat(asientos.extrasPara(business, pack(true), precios)).contains(List.of());
    }

    @Test
    void extrasPara_preferenteSinPrecioEnCatalogo_noSePuedeVender() {
        assertThat(asientos.extrasPara(a(10, "A"), pack(false), Map.of())).isEmpty();
        assertThat(asientos.extrasPara(a(5, "A"), pack(false), Map.of())).contains(List.of());
    }

    @Test
    void asignar_reemplazaSoloLosExtrasDeAsientoYConservaLosServicios() {
        Boleto boleto = Boleto.builder().id(1L).extras(new ArrayList<>()).build();
        boleto.addExtra(ReservaFixtures.extra(60, TipoExtra.EQUIPAJE_BODEGA, 15000), 1, BigDecimal.valueOf(15000));

        asientos.asignar(boleto, a(10, "A"), List.of(preferente));
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo())
                .containsExactly(TipoExtra.EQUIPAJE_BODEGA, TipoExtra.ASIENTO_PREFERENTE);

        asientos.asignar(boleto, a(10, "B"), List.of(ReservaFixtures.extra(51, TipoExtra.ASIENTO_PREFERENTE, 5500)));
        assertThat(boleto.getExtras()).hasSize(2);
        assertThat(boleto.getExtras().get(1).getPrecioUnitario()).isEqualByComparingTo("5500");

        asientos.asignar(boleto, a(5, "A"), List.of());
        assertThat(boleto.getAsientoId().getFila()).isEqualTo(5);
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);

        asientos.asignar(boleto, null, List.of());
        assertThat(boleto.getAsientoId()).isNull();
        assertThat(boleto.getExtras()).extracting(be -> be.getExtra().getTipo()).containsExactly(TipoExtra.EQUIPAJE_BODEGA);
    }

    private static PackTarifario pack(boolean incluyeSeleccion) {
        PackTarifario pack = ReservaFixtures.packLight();
        pack.setIncluyeSeleccionAsiento(incluyeSeleccion);
        return pack;
    }

    private Asiento a(int fila, String letra) {
        return aeronave.stream().filter(x -> x.getFila() == fila && x.getLetra().equals(letra)).findFirst().orElseThrow();
    }

    private static List<Asiento> aeronave() {
        List<Asiento> lista = new ArrayList<>();
        long id = 1;
        for (String letra : List.of("A", "C", "D", "F")) {
            lista.add(Asiento.builder().id(id++).fila(1).letra(letra).claseCabina(ClaseCabina.BUSINESS).esPreferente(true).build());
        }
        for (int fila : List.of(5, 6, 10)) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                lista.add(Asiento.builder().id(id++).fila(fila).letra(letra).claseCabina(ClaseCabina.ECONOMY)
                        .esPreferente(fila == 10).build());
            }
        }
        return lista;
    }
}
