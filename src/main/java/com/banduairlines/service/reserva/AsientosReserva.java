package com.banduairlines.service.reserva;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.ExtraRepository;

/**
 * Reglas de asientos que comparten los endpoints 3, 5 y 6 de RESERVA.
 * <ul>
 *   <li>Precio: el asiento normal no tiene costo. El preferente cobra el Extra ASIENTO_PREFERENTE del catálogo
 *       de ADMIN solo si el pack no incluye selección de asiento; con selección incluida es gratis.</li>
 *   <li>Distribución: las columnas salen de las letras de la aeronave; el pasillo va al medio (A B C | D E F).</li>
 *   <li>El cobro del asiento se guarda como BoletoExtra, igual que los demás extras, para que la calculadora lo sume.</li>
 * </ul>
 */
@Component
public class AsientosReserva {

    public static final String CATEGORIA_PREFERENTE = "PREFERENTE";
    public static final String CATEGORIA_ESTANDAR = "ESTANDAR";

    private static final Set<TipoExtra> TIPOS_ASIENTO = Set.of(TipoExtra.ASIENTO_PREFERENTE);

    private final ExtraRepository extraRepository;

    public AsientosReserva(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    /** Precio vigente del asiento preferente en el catálogo de ADMIN (el primero activo). */
    public Map<TipoExtra, Extra> preciosVigentes() {
        Map<TipoExtra, Extra> precios = new EnumMap<>(TipoExtra.class);
        for (Extra extra : extraRepository.findByActivoTrueOrderByIdAsc()) {
            if (TIPOS_ASIENTO.contains(extra.getTipo())) {
                precios.putIfAbsent(extra.getTipo(), extra);
            }
        }
        return precios;
    }

    /**
     * Extras que se cobran por elegir este asiento con este pack. Vacío = sin costo.
     * Optional.empty() = es un preferente que se cobra pero falta su precio en el catálogo, así que no se puede vender.
     */
    public Optional<List<Extra>> extrasPara(Asiento asiento, PackTarifario pack, Map<TipoExtra, Extra> precios) {
        if (!Boolean.TRUE.equals(asiento.getEsPreferente()) || Boolean.TRUE.equals(pack.getIncluyeSeleccionAsiento())) {
            return Optional.of(List.of());
        }
        Extra preferente = precios.get(TipoExtra.ASIENTO_PREFERENTE);
        return preferente == null ? Optional.empty() : Optional.of(List.of(preferente));
    }

    public static long precio(List<Extra> extras) {
        return extras.stream().mapToLong(e -> CalculadoraTotalReserva.clp(e.getPrecio())).sum();
    }

    /** El asiento no tiene costo adicional para este pack. */
    public static boolean incluidoEnPack(Asiento asiento, PackTarifario pack) {
        return !Boolean.TRUE.equals(asiento.getEsPreferente()) || Boolean.TRUE.equals(pack.getIncluyeSeleccionAsiento());
    }

    public static String categoria(Asiento asiento) {
        return Boolean.TRUE.equals(asiento.getEsPreferente()) ? CATEGORIA_PREFERENTE : CATEGORIA_ESTANDAR;
    }

    public static String codigo(Asiento asiento) {
        return asiento.getFila() + asiento.getLetra();
    }

    /** Letras de la aeronave en orden (A, B, C, ...). */
    public static List<String> columnas(List<Asiento> asientos) {
        Set<String> letras = new TreeSet<>();
        asientos.forEach(a -> letras.add(a.getLetra()));
        return List.copyOf(letras);
    }

    /** El modelo de ADMIN no guarda pasillos: con columnas pares se asume un pasillo central. */
    public static List<String> pasillosDespuesDe(List<String> columnas) {
        if (columnas.size() < 4 || columnas.size() % 2 != 0) {
            return List.of();
        }
        return List.of(columnas.get(columnas.size() / 2 - 1));
    }

    /**
     * true si todos los asientos están en la misma fila, uno al lado del otro y sin un pasillo entre medio.
     * La vecindad se mide con las letras que realmente tiene esa fila (en BUSINESS, A y C son vecinos).
     */
    public static boolean contiguos(List<Asiento> grupo, List<Asiento> asientosDeLaAeronave) {
        if (grupo.size() <= 1) {
            return true;
        }
        Integer fila = grupo.get(0).getFila();
        if (grupo.stream().anyMatch(a -> !a.getFila().equals(fila))) {
            return false;
        }
        List<String> letrasFila = asientosDeLaAeronave.stream()
                .filter(a -> a.getFila().equals(fila))
                .map(Asiento::getLetra)
                .sorted()
                .toList();
        List<String> pasillos = pasillosDespuesDe(columnas(asientosDeLaAeronave));
        List<Integer> posiciones = grupo.stream()
                .map(a -> letrasFila.indexOf(a.getLetra()))
                .sorted(Comparator.naturalOrder())
                .toList();
        for (int i = 1; i < posiciones.size(); i++) {
            int anterior = posiciones.get(i - 1);
            if (posiciones.get(i) != anterior + 1 || pasillos.contains(letrasFila.get(anterior))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deja el boleto con este asiento (o sin asiento si es null) y con exactamente estos extras de asiento.
     * Los extras que se mantienen se actualizan en el mismo registro: Hibernate inserta antes de borrar,
     * así que borrar y volver a crear la misma clave compuesta fallaría (ver ReservaServiciosServiceImpl).
     */
    public void asignar(Boleto boleto, Asiento asiento, List<Extra> extras) {
        boleto.setAsientoId(asiento);
        List<Extra> pendientes = new ArrayList<>(asiento == null ? List.of() : extras);
        Iterator<BoletoExtra> it = boleto.getExtras().iterator();
        while (it.hasNext()) {
            BoletoExtra actual = it.next();
            if (!TIPOS_ASIENTO.contains(actual.getExtra().getTipo())) {
                continue;
            }
            Extra mismo = pendientes.stream()
                    .filter(e -> e.getId().equals(actual.getExtra().getId()))
                    .findFirst().orElse(null);
            if (mismo == null) {
                it.remove();
            } else {
                pendientes.remove(mismo);
                actual.setCantidad(1);
                actual.setPrecioUnitario(mismo.getPrecio());
            }
        }
        pendientes.forEach(extra -> boleto.addExtra(extra, 1, extra.getPrecio()));
    }
}
