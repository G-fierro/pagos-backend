package com.banduairlines.devdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Ruta;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.Estado;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.repository.RutaRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.VueloRepository;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AeropuertoRepository aeropuertoRepository;
    private final RutaRepository rutaRepository;
    private final AvionRepository aeronaveRepository;
    private final AsientoRepository asientoRepository;
    private final PackTarifarioRepository packTarifarioRepository;
    private final VueloRepository vueloRepository;
    private final TarifaVueloRepository tarifaVueloRepository;

    public DataSeeder(AeropuertoRepository aeropuertoRepository,
                      RutaRepository rutaRepository,
                      AvionRepository aeronaveRepository,
                      AsientoRepository asientoRepository,
                      PackTarifarioRepository packTarifarioRepository,
                      VueloRepository vueloRepository,
                      TarifaVueloRepository tarifaVueloRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.rutaRepository = rutaRepository;
        this.aeronaveRepository = aeronaveRepository;
        this.asientoRepository = asientoRepository;
        this.packTarifarioRepository = packTarifarioRepository;
        this.vueloRepository = vueloRepository;
        this.tarifaVueloRepository = tarifaVueloRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (aeropuertoRepository.count() > 0) {
            log.info("DataSeeder: las tablas ya contienen datos. Se omite la carga inicial.");
            return;
        }

        log.warn("DATOS DE PRUEBA: solo para desarrollo local, no usar en servidor");
        log.info("DataSeeder: iniciando carga de datos iniciales...");

        // 1. Aeropuertos (~6 de Chile)
        Aeropuerto scl = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("SCL").nombre("Aeropuerto Internacional Arturo Merino Benítez")
                .ciudad("Santiago").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto anf = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("ANF").nombre("Aeropuerto Andrés Sabella")
                .ciudad("Antofagasta").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto cjc = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("CJC").nombre("Aeropuerto El Loa")
                .ciudad("Calama").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto ccp = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("CCP").nombre("Aeropuerto Carriel Sur")
                .ciudad("Concepción").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto pmc = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("PMC").nombre("Aeropuerto El Tepual")
                .ciudad("Puerto Montt").pais("Chile").zonaHoraria("America/Santiago").build());

        Aeropuerto puq = aeropuertoRepository.save(Aeropuerto.builder()
                .codigoIata("PUQ").nombre("Aeropuerto Presidente Carlos Ibáñez del Campo")
                .ciudad("Punta Arenas").pais("Chile").zonaHoraria("America/Punta_Arenas").build());

        // 2. Rutas en ambos sentidos
        List<Ruta> rutas = new ArrayList<>();
        rutas.add(crearRuta(scl, anf, 1100, 120));
        rutas.add(crearRuta(anf, scl, 1100, 120));

        rutas.add(crearRuta(scl, cjc, 1230, 125));
        rutas.add(crearRuta(cjc, scl, 1230, 125));

        rutas.add(crearRuta(scl, ccp, 430, 60));
        rutas.add(crearRuta(ccp, scl, 430, 60));

        rutas.add(crearRuta(scl, pmc, 915, 105));
        rutas.add(crearRuta(pmc, scl, 915, 105));

        rutas.add(crearRuta(scl, puq, 2190, 200));
        rutas.add(crearRuta(puq, scl, 2190, 200));

        rutas.add(crearRuta(pmc, puq, 1310, 130));
        rutas.add(crearRuta(puq, pmc, 1310, 130));
        rutas = rutaRepository.saveAll(rutas);

        // 3. Aviones (1-2) con asientos
        Avion avion1 = aeronaveRepository.save(Avion.builder()
                .matricula("CC-BAN").modelo("Airbus A320neo").estado(Estado.ACTIVA).build());

        Avion avion2 = aeronaveRepository.save(Avion.builder()
                .matricula("CC-BDU").modelo("Airbus A321neo").estado(Estado.ACTIVA).build());

        generarAsientos(avion1);
        generarAsientos(avion2);

        // 4. Packs tarifarios
        PackTarifario packLight = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Light").descripcion("Viaja ligero sin equipaje en bodega")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(0).kgPorPiezaBodega(0)
                .incluyeSeleccionAsiento(false).incluyeEmbarquePrioritario(false).permiteCambios(false)
                .multiplicadorMillas(new BigDecimal("1.00")).activo(true).build());

        PackTarifario packPlus = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Plus").descripcion("Incluye 1 pieza de bodega y selección de asiento")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(1).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(true).incluyeEmbarquePrioritario(false).permiteCambios(true)
                .multiplicadorMillas(new BigDecimal("1.25")).activo(true).build());

        PackTarifario packTop = packTarifarioRepository.save(PackTarifario.builder()
                .nombre("Top").descripcion("Todo incluido: 2 piezas en bodega, embarque prioritario y cambios gratis")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(2).kgPorPiezaBodega(23)
                .incluyeSeleccionAsiento(true).incluyeEmbarquePrioritario(true).permiteCambios(true)
                .multiplicadorMillas(new BigDecimal("1.50")).activo(true).build());

        // 5. Vuelos en los próximos 14 días con tarifas
        LocalDate hoy = LocalDate.now();
        List<Avion> aviones = List.of(avion1, avion2);

        int contadorVuelo = 100;
        for (int dia = 1; dia <= 14; dia++) {
            LocalDate fechaVuelo = hoy.plusDays(dia);

            for (int r = 0; r < rutas.size(); r++) {
                Ruta ruta = rutas.get(r);
                Avion avion = aviones.get((dia + r) % aviones.size());
                contadorVuelo++;

                LocalTime horaSalida = LocalTime.of(7 + (r * 2) % 14, (r * 15) % 60);
                LocalDateTime salida = LocalDateTime.of(fechaVuelo, horaSalida);
                LocalDateTime llegada = salida.plusMinutes(ruta.getDuracionMin());

                Vuelo vuelo = vueloRepository.save(Vuelo.builder()
                        .codigoVuelo("BA-" + contadorVuelo)
                        .ruta(ruta)
                        .aeronave(avion)
                        .salidaProgramada(salida)
                        .llegadaProgramada(llegada)
                        .estado(EstadoVuelo.PROGRAMADO)
                        .build());

                // Tarifas asociadas al vuelo
                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packLight)
                        .precio(new BigDecimal("29990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packPlus)
                        .precio(new BigDecimal("45990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(packTop)
                        .precio(new BigDecimal("69990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.PREMIUM_ECONOMY).pack(packPlus)
                        .precio(new BigDecimal("65990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.PREMIUM_ECONOMY).pack(packTop)
                        .precio(new BigDecimal("89990.00")).build());

                tarifaVueloRepository.save(TarifaVuelo.builder()
                        .vuelo(vuelo).claseCabina(ClaseCabina.BUSINESS).pack(packTop)
                        .precio(new BigDecimal("139990.00")).build());
            }
        }

        log.info("DataSeeder: carga inicial completada exitosamente.");
    }

    private Ruta crearRuta(Aeropuerto origen, Aeropuerto destino, int distanciaKm, int duracionMin) {
        return Ruta.builder()
                .origen(origen)
                .destino(destino)
                .distanciaKm(distanciaKm)
                .duracionMin(duracionMin)
                .activa(true)
                .build();
    }

    private void generarAsientos(Avion avion) {
        List<Asiento> asientos = new ArrayList<>();
        // Filas 1-2: BUSINESS (A, C, D, F)
        for (int fila = 1; fila <= 2; fila++) {
            for (String letra : List.of("A", "C", "D", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.BUSINESS).esPreferente(true).build());
            }
        }
        // Filas 3-4: PREMIUM_ECONOMY (A, B, C, D, E, F)
        for (int fila = 3; fila <= 4; fila++) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.PREMIUM_ECONOMY).esPreferente(true).build());
            }
        }
        // Filas 5-15: ECONOMY (A, B, C, D, E, F)
        for (int fila = 5; fila <= 15; fila++) {
            boolean preferente = (fila == 10); // salida de emergencia
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientos.add(Asiento.builder()
                        .aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.ECONOMY).esPreferente(preferente).build());
            }
        }
        asientoRepository.saveAll(asientos);
    }
}
