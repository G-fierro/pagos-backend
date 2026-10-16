package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;
import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ReservaCreacionService;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String VALIDO = """
            {"alternativaId":"ALT-1","tramos":[{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
             "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
            """;

    @Mock private ReservaCreacionService reservaCreacionService;
    @Mock private ReservaConsultaService reservaConsultaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaCreacionService, reservaConsultaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crearReserva_valida_responde201ConReservaCreada() throws Exception {
        when(reservaCreacionService.crear(any(CrearReservaRequest.class))).thenReturn(new ReservaCreadaResponse(
                42L, EstadoReserva.EN_CURSO, "tok", OffsetDateTime.of(2026, 11, 1, 18, 15, 0, 0, ZoneOffset.UTC),
                new TotalResponse(List.of(new TotalResponse.Linea("TARIFA", "Tarifa (1 boleto)", 29990)), 0, 29990, "CLP")));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservaId").value(42))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.compraToken").value("tok"))
                .andExpect(jsonPath("$.total.totalCLP").value(29990));
    }

    @Test
    void crearReserva_sinTramos_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[],"pack":"Light","cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.details[0].campo").value("tramos"));
        verifyNoInteractions(reservaCreacionService);
    }

    @Test
    void crearReserva_ceroPasajeros_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":0,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void crearReserva_tramoRepetido_responde400Validacion() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"ECONOMY"},{"tramoId":10,"cabina":"ECONOMY"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void crearReserva_cabinaInexistente_responde400PeticionMalformada() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tramos":[{"tramoId":10,"cabina":"PRIMERA"}],"pack":"Light",
                         "cantidadPasajeros":{"adultos":1,"ninos":0,"bebes":0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
    }

    @Test
    void crearReserva_bebesSinAdulto_responde422ConSuCodigo() throws Exception {
        when(reservaCreacionService.crear(any(CrearReservaRequest.class)))
                .thenThrow(new ReglaNegocioException("Cada bebé debe viajar con un adulto").conCodigo("BEBES_SIN_ADULTO"));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("BEBES_SIN_ADULTO"));
    }

    @Test
    void crearReserva_sinDisponibilidad_responde409ConSuCodigo() throws Exception {
        when(reservaCreacionService.crear(any(CrearReservaRequest.class)))
                .thenThrow(new ConflictoException("No quedan asientos").conCodigo("SIN_DISPONIBILIDAD"));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIN_DISPONIBILIDAD"));
    }

    // ---------------------------------------------------------------- endpoint 2

    @Test
    void obtenerReserva_conToken_responde200ConLaReserva() throws Exception {
        when(reservaConsultaService.obtener(42L, "tok")).thenReturn(new ReservaResponse(
                42L, null, EstadoReserva.EN_CURSO, null, "Light",
                new ReservaResponse.CantidadPasajeros(1, 0, 0), List.of(), List.of(), List.of(), List.of(), null, null,
                new TotalResponse(List.of(), 0, 29990, "CLP")));

        mockMvc.perform(get("/api/v1/reservas/42").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(42))
                .andExpect(jsonPath("$.codigoReserva").doesNotExist())
                .andExpect(jsonPath("$.pack").value("Light"))
                .andExpect(jsonPath("$.cantidadPasajeros.adultos").value(1))
                .andExpect(jsonPath("$.cantidadPasajeros.alMenosUnPasajero").doesNotExist())
                .andExpect(jsonPath("$.total.totalCLP").value(29990));
    }

    @Test
    void obtenerReserva_sinCredencial_responde401() throws Exception {
        when(reservaConsultaService.obtener(42L, null))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Falta el X-Compra-Token"));

        mockMvc.perform(get("/api/v1/reservas/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void obtenerReserva_inexistente_responde404() throws Exception {
        when(reservaConsultaService.obtener(99L, "tok")).thenThrow(new RecursoNoEncontradoException("Reserva", 99L));

        mockMvc.perform(get("/api/v1/reservas/99").header("X-Compra-Token", "tok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void obtenerReserva_idNoNumerico_responde400() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/abc").header("X-Compra-Token", "tok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
    }
}
