package com.banduairlines.controller.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.banduairlines.config.AjusteEsquemaReservas;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Asiento;
import com.banduairlines.model.Avion;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Extra;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Ruta;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.AsientoRepository;
import com.banduairlines.repository.AvionRepository;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.repository.PackTarifarioRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.RutaRepository;
import com.banduairlines.repository.TarifaVueloRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.security.UsuarioAutenticado;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

/**
 * Endpoints 1 al 6 de RESERVA de punta a punta: HTTP → controlador → servicio → JPA → base H2 en memoria.
 * Cada prueba corre en su propia transacción que se revierte al final, así no dependen del orden ni de datos manuales.
 * AjusteEsquemaReservas se reemplaza por un mock porque usa catálogos propios de PostgreSQL.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reserva_endpoints;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "app.security.enabled=false",
        "app.seed.enabled=false",
        "app.reserva.expiracion-intervalo-ms=3600000"
})
@Transactional
class ReservaEndpoints1a6IntegracionTest {

    @MockitoBean private AjusteEsquemaReservas ajusteEsquemaReservas;

    @Autowired private WebApplicationContext context;
    @Autowired private EntityManager em;
    @Autowired private AeropuertoRepository aeropuertoRepository;
    @Autowired private RutaRepository rutaRepository;
    @Autowired private AvionRepository avionRepository;
    @Autowired private AsientoRepository asientoRepository;
    @Autowired private PackTarifarioRepository packTarifarioRepository;
    @Autowired private VueloRepository vueloRepository;
    @Autowired private TarifaVueloRepository tarifaVueloRepository;
    @Autowired private ExtraRepository extraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private TramoReservaRepository tramoReservaRepository;
    @Autowired private PasajeroRepository pasajeroRepository;
    @Autowired private BoletoRepository boletoRepository;

    private MockMvc mockMvc;
    private Vuelo vuelo;
    private LocalDate fechaVuelo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        Aeropuerto scl = aeropuertoRepository.save(Aeropuerto.builder().codigoIata("SCL").nombre("Arturo Merino Benítez").ciudad("Santiago").build());
        Aeropuerto pmc = aeropuertoRepository.save(Aeropuerto.builder().codigoIata("PMC").nombre("El Tepual").ciudad("Puerto Montt").build());
        Ruta ruta = rutaRepository.save(Ruta.builder().origen(scl).destino(pmc).distanciaKm(912).duracionMin(105).build());
        Avion avion = avionRepository.save(Avion.builder().matricula("CC-TEST").modelo("A320neo").build());
        // 3 filas ECONOMY de 6 asientos = 18 plazas; la fila 3 es preferente.
        for (int fila = 1; fila <= 3; fila++) {
            for (String letra : List.of("A", "B", "C", "D", "E", "F")) {
                asientoRepository.save(Asiento.builder().aeronave(avion).fila(fila).letra(letra)
                        .claseCabina(ClaseCabina.ECONOMY).esPreferente(fila == 3).build());
            }
        }
        PackTarifario light = packTarifarioRepository.save(PackTarifario.builder().nombre("Light")
                .incluyeEquipajeCabina(true).piezasBodegaIncluidas(0).kgPorPiezaBodega(0)
                .incluyeSeleccionAsiento(false).incluyeEmbarquePrioritario(false).permiteCambios(false).build());
        LocalDateTime salida = LocalDateTime.now(ZoneOffset.UTC).plusDays(5).withNano(0);
        fechaVuelo = salida.toLocalDate();
        vuelo = vueloRepository.save(Vuelo.builder().codigoVuelo("BA-900").ruta(ruta).aeronave(avion)
                .salidaProgramada(salida).llegadaProgramada(salida.plusMinutes(105)).estado(EstadoVuelo.PROGRAMADO).build());
        tarifaVueloRepository.save(TarifaVuelo.builder().vuelo(vuelo).claseCabina(ClaseCabina.ECONOMY).pack(light)
                .precio(new BigDecimal("29990.00")).build());
        extraRepository.save(Extra.builder().tipo(TipoExtra.ASIENTO_PREFERENTE).nombre("Asiento preferente")
                .precio(new BigDecimal("5000.00")).build());
        em.flush();
    }

    // ---------------------------------------------------------------- flujo completo

    @Test
    void flujoInvitado_crearConsultarPasajerosContactoMapaYAsientos_persisteCadaPaso() throws Exception {
        // 1. POST /reservas
        String creada = mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(crearReserva(1, 1, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.compraToken").isNotEmpty())
                .andExpect(jsonPath("$.total.totalCLP").value(3 * 29990))
                .andReturn().getResponse().getContentAsString();
        long reservaId = numero(creada, "$.reservaId");
        String token = JsonPath.read(creada, "$.compraToken");

        sincronizar();
        Reserva enBase = reservaRepository.findById(reservaId).orElseThrow();
        assertThat(enBase.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(enBase.getEmailContacto()).isNull();
        assertThat(enBase.getCantidadNinos()).isEqualTo(1);
        assertThat(tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(reservaId)).singleElement()
                .satisfies(t -> assertThat(t.getPrecioTarifa()).isEqualByComparingTo("29990"));

        // 2. GET /reservas/{id}
        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId).header("X-Compra-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itinerario[0].origen").value("SCL"))
                .andExpect(jsonPath("$.itinerario[0].destino").value("PMC"))
                .andExpect(jsonPath("$.pasajeros").isEmpty())
                .andExpect(jsonPath("$.total.totalCLP").value(3 * 29990));

        // 3. PUT /pasajeros
        String pasajeros = mockMvc.perform(put("/api/v1/reservas/{id}/pasajeros", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(familia("Ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasajeros.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        long idAdulto = numero(pasajeros, "$.pasajeros[0].pasajeroId");
        long idNino = numero(pasajeros, "$.pasajeros[1].pasajeroId");
        assertThat(numero(pasajeros, "$.pasajeros[1].adultoResponsableId")).isEqualTo(idAdulto);

        sincronizar();
        assertThat(pasajeroRepository.findByReserva_IdOrderByIdAsc(reservaId)).hasSize(3);
        assertThat(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reservaId)).hasSize(3);

        // 4. PUT /contacto
        mockMvc.perform(put("/api/v1/reservas/{id}/contacto", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Ana Pérez\",\"correo\":\"Ana.Perez@Correo.cl\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana.perez@correo.cl"));
        sincronizar();
        assertThat(reservaRepository.findById(reservaId).orElseThrow().getEmailContacto()).isEqualTo("ana.perez@correo.cl");

        // 5. GET mapa
        mockMvc.perform(get("/api/v1/reservas/{id}/tramos/{t}/asientos", reservaId, vuelo.getId()).header("X-Compra-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnas.length()").value(6))
                .andExpect(jsonPath("$.pasillosDespuesDe[0]").value("C"))
                .andExpect(jsonPath("$.filas[0].asientos[0].codigo").value("1A"))
                .andExpect(jsonPath("$.filas[0].asientos[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.filas[0].asientos[0].precioCLP").value(0))
                .andExpect(jsonPath("$.filas[0].asientos[0].incluidoEnPack").value(true))
                .andExpect(jsonPath("$.filas[2].asientos[0].categoria").value("PREFERENTE"))
                .andExpect(jsonPath("$.filas[2].asientos[0].precioCLP").value(5000));

        // 6. PUT /asientos: adulto 3A y niño 3B, preferentes con pack Light (el bebé no ocupa asiento)
        mockMvc.perform(put("/api/v1/reservas/{id}/asientos", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(selecciones(idAdulto, "3A", idNino, "3B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.totalCLP").value(3 * 29990 + 2 * 5000));
        sincronizar();
        Boleto boletoAdulto = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reservaId).stream()
                .filter(b -> b.getPasajeroId().getId().equals(idAdulto)).findFirst().orElseThrow();
        assertThat(boletoAdulto.getAsientoId().getFila()).isEqualTo(3);
        assertThat(boletoAdulto.getAsientoId().getLetra()).isEqualTo("A");
        assertThat(boletoAdulto.getExtras()).singleElement()
                .satisfies(be -> assertThat(be.getExtra().getTipo()).isEqualTo(TipoExtra.ASIENTO_PREFERENTE));
        assertThat(reservaRepository.findById(reservaId).orElseThrow().getTotal()).isEqualByComparingTo("99970");

        mockMvc.perform(get("/api/v1/reservas/{id}/tramos/{t}/asientos", reservaId, vuelo.getId()).header("X-Compra-Token", token))
                .andExpect(jsonPath("$.filas[2].asientos[0].estado").value("SELECCIONADO"));

        // Reenviar pasajeros con un nombre corregido: mismos registros y asientos conservados.
        mockMvc.perform(put("/api/v1/reservas/{id}/pasajeros", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(familia("Anita")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasajeros[0].pasajeroId").value(idAdulto))
                .andExpect(jsonPath("$.pasajeros[0].nombres").value("Anita"));
        sincronizar();
        assertThat(pasajeroRepository.findByReserva_IdOrderByIdAsc(reservaId)).hasSize(3);
        assertThat(boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reservaId))
                .filteredOn(b -> b.getAsientoId() != null).hasSize(2);

        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId).header("X-Compra-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contacto.correo").value("ana.perez@correo.cl"))
                .andExpect(jsonPath("$.asientos[0].asiento").value("3A"))
                .andExpect(jsonPath("$.total.totalCLP").value(99970));
    }

    // ---------------------------------------------------------------- autenticación y autorización

    @Test
    void invitado_sinTokenOConTokenInvalido_responde401() throws Exception {
        long reservaId = numero(crearComoInvitado(1, 0, 0), "$.reservaId");

        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mockMvc.perform(put("/api/v1/reservas/{id}/contacto", reservaId).header("X-Compra-Token", "token-falso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Ana\",\"correo\":\"a@correo.cl\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOtraReserva_noAbreEstaReserva() throws Exception {
        long reservaA = numero(crearComoInvitado(1, 0, 0), "$.reservaId");
        String tokenB = JsonPath.read(crearComoInvitado(1, 0, 0), "$.compraToken");

        mockMvc.perform(get("/api/v1/reservas/{id}", reservaA).header("X-Compra-Token", tokenB))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pasajeroRegistrado_suReservaNoEntregaTokenYOtroUsuarioRecibe403() throws Exception {
        Usuario duena = usuario("duena@correo.cl");
        Usuario otro = usuario("otro@correo.cl");

        String creada = mockMvc.perform(post("/api/v1/reservas").with(como(duena))
                        .contentType(MediaType.APPLICATION_JSON).content(crearReserva(1, 0, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compraToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long reservaId = numero(creada, "$.reservaId");
        sincronizar();
        assertThat(reservaRepository.findById(reservaId).orElseThrow().getUsuario().getId()).isEqualTo(duena.getId());

        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId).with(como(duena)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId).with(como(otro)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        mockMvc.perform(get("/api/v1/reservas/{id}", reservaId))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- errores de negocio y recursos inexistentes

    @Test
    void asientoBloqueadoPorOtraCompra_noSePuedeTomarYApareceNoDisponible() throws Exception {
        String a = crearComoInvitado(1, 0, 0);
        long reservaA = numero(a, "$.reservaId");
        String tokenA = JsonPath.read(a, "$.compraToken");
        long adultoA = registrarUnAdulto(reservaA, tokenA, "11111111-1");
        mockMvc.perform(put("/api/v1/reservas/{id}/asientos", reservaA).header("X-Compra-Token", tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(selecciones(adultoA, "2C")))
                .andExpect(status().isOk());

        String b = crearComoInvitado(1, 0, 0);
        long reservaB = numero(b, "$.reservaId");
        String tokenB = JsonPath.read(b, "$.compraToken");
        long adultoB = registrarUnAdulto(reservaB, tokenB, "22222222-2");

        mockMvc.perform(put("/api/v1/reservas/{id}/asientos", reservaB).header("X-Compra-Token", tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(selecciones(adultoB, "2C")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASIENTO_NO_DISPONIBLE"));
        mockMvc.perform(get("/api/v1/reservas/{id}/tramos/{t}/asientos", reservaB, vuelo.getId()).header("X-Compra-Token", tokenB))
                .andExpect(jsonPath("$.filas[1].asientos[2].codigo").value("2C"))
                .andExpect(jsonPath("$.filas[1].asientos[2].estado").value("NO_DISPONIBLE"));
    }

    @Test
    void reservaVencida_noAceptaCambios() throws Exception {
        String creada = crearComoInvitado(1, 0, 0);
        long reservaId = numero(creada, "$.reservaId");
        String token = JsonPath.read(creada, "$.compraToken");
        reservaRepository.findById(reservaId).orElseThrow().setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        em.flush();

        mockMvc.perform(put("/api/v1/reservas/{id}/contacto", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Ana\",\"correo\":\"a@correo.cl\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVA_EXPIRADA"));
    }

    @Test
    void sinCupoEnLaCabina_responde409YNoGuardaNada() throws Exception {
        long antes = reservaRepository.count();

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(crearReserva(19, 0, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_DISPONIBILIDAD"));
        assertThat(reservaRepository.count()).isEqualTo(antes);
    }

    @Test
    void masBebesQueAdultos_responde422() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(crearReserva(1, 0, 2)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("BEBES_SIN_ADULTO"));
    }

    @Test
    void recursosInexistentes_responden404() throws Exception {
        String creada = crearComoInvitado(1, 0, 0);
        long reservaId = numero(creada, "$.reservaId");
        String token = JsonPath.read(creada, "$.compraToken");
        registrarUnAdulto(reservaId, token, "11111111-1");

        mockMvc.perform(get("/api/v1/reservas/{id}", 999_999L).header("X-Compra-Token", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reservas/{id}/tramos/{t}/asientos", reservaId, 999_999L).header("X-Compra-Token", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/reservas/{id}/asientos", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(selecciones(999_999L, "1A")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(crearReserva(1, 0, 0).replace("\"tramoId\":" + vuelo.getId(), "\"tramoId\":999999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void pasajeroConTipoQueNoCoincideConLaEdad_responde422YNoGuarda() throws Exception {
        String creada = crearComoInvitado(1, 0, 0);
        long reservaId = numero(creada, "$.reservaId");
        String token = JsonPath.read(creada, "$.compraToken");

        mockMvc.perform(put("/api/v1/reservas/{id}/pasajeros", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pasajero("a1", "ADULTO", fechaVuelo.minusYears(10), "11111111-1", null)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("TIPO_NO_COINCIDE_EDAD"));
        sincronizar();
        assertThat(pasajeroRepository.findByReserva_IdOrderByIdAsc(reservaId)).isEmpty();
    }

    // ---------------------------------------------------------------- utilidades

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    private String crearComoInvitado(int adultos, int ninos, int bebes) throws Exception {
        return mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(crearReserva(adultos, ninos, bebes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long registrarUnAdulto(long reservaId, String token, String documento) throws Exception {
        String r = mockMvc.perform(put("/api/v1/reservas/{id}/pasajeros", reservaId).header("X-Compra-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pasajero("a1", "ADULTO", LocalDate.of(1990, 3, 15), documento, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return numero(r, "$.pasajeros[0].pasajeroId");
    }

    private String crearReserva(int adultos, int ninos, int bebes) {
        return "{\"alternativaId\":\"ALT-IT\",\"tramos\":[{\"tramoId\":" + vuelo.getId() + ",\"cabina\":\"ECONOMY\"}],"
                + "\"pack\":\"light\",\"cantidadPasajeros\":{\"adultos\":" + adultos + ",\"ninos\":" + ninos
                + ",\"bebes\":" + bebes + "}}";
    }

    private String familia(String nombreAdulto) {
        return "{\"pasajeros\":["
                + persona("a1", "ADULTO", nombreAdulto, LocalDate.of(1990, 3, 15), "11111111-1", null) + ","
                + persona("n1", "NINO", "Leo", fechaVuelo.minusYears(8), "33333333-3", "a1") + ","
                + persona("b1", "BEBE", "Sol", fechaVuelo.minusMonths(8), "55555555-5", "a1") + "]}";
    }

    private static String pasajero(String ref, String tipo, LocalDate nacimiento, String documento, String responsable) {
        return "{\"pasajeros\":[" + persona(ref, tipo, "Ana", nacimiento, documento, responsable) + "]}";
    }

    private static String persona(String ref, String tipo, String nombres, LocalDate nacimiento, String documento, String responsable) {
        return "{\"ref\":\"" + ref + "\",\"tipo\":\"" + tipo + "\",\"nombres\":\"" + nombres + "\",\"apellidos\":\"Pérez\","
                + "\"fechaNacimiento\":\"" + nacimiento + "\",\"tipoDocumento\":\"RUT\",\"numeroDocumento\":\"" + documento + "\","
                + "\"nacionalidad\":\"Chilena\""
                + (responsable != null ? ",\"adultoResponsableRef\":\"" + responsable + "\"" : "") + "}";
    }

    private String selecciones(Object... pares) {
        StringBuilder sb = new StringBuilder("{\"selecciones\":[");
        for (int i = 0; i < pares.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"pasajeroId\":").append(pares[i]).append(",\"tramoId\":").append(vuelo.getId()).append(",\"asiento\":\"").append(pares[i + 1]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    private Usuario usuario(String email) {
        Usuario u = usuarioRepository.save(Usuario.builder().email(email).passwordHash("x").nombre("Test").apellido("Usuario").build());
        em.flush();
        return u;
    }

    private static RequestPostProcessor como(Usuario usuario) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), RolUsuario.PASAJERO), null,
                List.of(new SimpleGrantedAuthority("ROLE_PASAJERO"))));
    }

    private static long numero(String json, String ruta) {
        return ((Number) JsonPath.read(json, ruta)).longValue();
    }
}
