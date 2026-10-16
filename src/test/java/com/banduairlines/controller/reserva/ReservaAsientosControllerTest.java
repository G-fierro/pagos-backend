package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.AsignacionAsiento;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;
import com.banduairlines.dto.reserva.TotalResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.service.reserva.ReservaAsientosService;

@ExtendWith(MockitoExtension.class)
class ReservaAsientosControllerTest {

    @Mock private ReservaAsientosService reservaAsientosService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaAsientosController(reservaAsientosService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------- endpoint 5

    @Test
    void obtenerMapa_responde200ConFilasYEstados() throws Exception {
        when(reservaAsientosService.obtenerMapa(1L, 10L, "tok")).thenReturn(new MapaAsientosResponse(10L, ClaseCabina.ECONOMY,
                List.of("A", "B", "C", "D", "E", "F"), List.of("C"),
                List.of(new MapaAsientosResponse.Fila(5, ClaseCabina.ECONOMY, List.of(
                        new MapaAsientosResponse.Asiento("5A", "A", "PREFERENTE", "DISPONIBLE", 5000, false))))));

        mockMvc.perform(get("/api/v1/reservas/1/tramos/10/asientos").header("X-Compra-Token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasillosDespuesDe[0]").value("C"))
                .andExpect(jsonPath("$.filas[0].asientos[0].codigo").value("5A"))
                .andExpect(jsonPath("$.filas[0].asientos[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.filas[0].asientos[0].precioCLP").value(5000));
    }

    @Test
    void obtenerMapa_tramoAjeno_responde404() throws Exception {
        when(reservaAsientosService.obtenerMapa(1L, 99L, "tok")).thenThrow(new RecursoNoEncontradoException("Tramo", 99L));

        mockMvc.perform(get("/api/v1/reservas/1/tramos/99/asientos").header("X-Compra-Token", "tok"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- endpoint 6

    @Test
    void seleccionar_valido_responde200ConAsignacionesYTotal() throws Exception {
        when(reservaAsientosService.seleccionar(eq(1L), eq("tok"), any(SeleccionarAsientosRequest.class)))
                .thenReturn(new AsientosAsignadosResponse(List.of(new AsignacionAsiento(7L, 10L, "5A", 5000)), null,
                        new TotalResponse(List.of(), 0, 34990, "CLP")));

        mockMvc.perform(put("/api/v1/reservas/1/asientos").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[{\"pasajeroId\":7,\"tramoId\":10,\"asiento\":\"5A\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignaciones[0].asiento").value("5A"))
                .andExpect(jsonPath("$.total.totalCLP").value(34990));
    }

    @Test
    void seleccionar_asientoNulo_esValidoParaContinuarSinAsiento() throws Exception {
        when(reservaAsientosService.seleccionar(eq(1L), eq("tok"), any(SeleccionarAsientosRequest.class)))
                .thenReturn(new AsientosAsignadosResponse(List.of(new AsignacionAsiento(7L, 10L, null, 0)), null,
                        new TotalResponse(List.of(), 0, 29990, "CLP")));

        mockMvc.perform(put("/api/v1/reservas/1/asientos").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[{\"pasajeroId\":7,\"tramoId\":10,\"asiento\":null}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void seleccionar_sinSelecciones_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/asientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
        verifyNoInteractions(reservaAsientosService);
    }

    @Test
    void seleccionar_formatoDeAsientoInvalido_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/asientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[{\"pasajeroId\":7,\"tramoId\":10,\"asiento\":\"ventana\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].campo").value("selecciones[0].asiento"));
    }

    @Test
    void seleccionar_mismoPasajeroDosVecesEnUnTramo_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/asientos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selecciones":[{"pasajeroId":7,"tramoId":10,"asiento":"5A"},
                                                {"pasajeroId":7,"tramoId":10,"asiento":"5B"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void seleccionar_asientoTomado_responde409ConSuCodigo() throws Exception {
        when(reservaAsientosService.seleccionar(eq(1L), eq("tok"), any(SeleccionarAsientosRequest.class)))
                .thenThrow(new ConflictoException("El asiento 5A ya fue tomado").conCodigo("ASIENTO_NO_DISPONIBLE"));

        mockMvc.perform(put("/api/v1/reservas/1/asientos").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[{\"pasajeroId\":7,\"tramoId\":10,\"asiento\":\"5A\"}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASIENTO_NO_DISPONIBLE"));
    }

    @Test
    void seleccionar_menorSeparado_responde422ConSuCodigo() throws Exception {
        when(reservaAsientosService.seleccionar(eq(1L), eq("tok"), any(SeleccionarAsientosRequest.class)))
                .thenThrow(new ReglaNegocioException("Los menores deben quedar junto a su adulto").conCodigo("MENOR_SEPARADO_DE_ADULTO"));

        mockMvc.perform(put("/api/v1/reservas/1/asientos").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selecciones\":[{\"pasajeroId\":8,\"tramoId\":10,\"asiento\":\"5D\"}]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("MENOR_SEPARADO_DE_ADULTO"));
    }
}
