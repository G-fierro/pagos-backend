package com.banduairlines.controller.admin;

import com.banduairlines.dto.admin.RutaRespuesta;
import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.service.admin.RutaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.banduairlines.exception.ConflictoException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RutaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RutaService servicio;

    @InjectMocks
    private RutaController controlador;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crearRecibeJsonYDevuelveUbicacion() throws Exception {
        RutaSolicitud solicitud = new RutaSolicitud(
                1L, 2L, 620, 85, true);

        RutaRespuesta respuesta = new RutaRespuesta(
                10L,
                1L, "SCL", "Santiago",
                2L, "ZCO", "Temuco",
                620, 85, true);

        when(servicio.crear(solicitud)).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/admin/rutas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "origenId": 1,
                            "destinoId": 2,
                            "distanciaKm": 620,
                            "duracionMin": 85,
                            "activa": true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/admin/rutas/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.origenCodigoIata").value("SCL"))
                .andExpect(jsonPath("$.destinoCodigoIata").value("ZCO"));

        verify(servicio).crear(solicitud);
    }

    @Test
    void cambiarEstadoRecibeFalseCorrectamente() throws Exception {
        RutaRespuesta respuesta = new RutaRespuesta(
                10L,
                1L, "SCL", "Santiago",
                2L, "ZCO", "Temuco",
                620, 85, false);

        when(servicio.cambiarEstado(10L, false))
                .thenReturn(respuesta);

        mockMvc.perform(patch("/api/v1/admin/rutas/10/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"activa": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        verify(servicio).cambiarEstado(10L, false);
    }

    @Test
    void cambiarEstadoRechazaCampoAusente() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/rutas/10/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath(
                        "$.details[?(@.campo == 'activa')]").exists());

        verifyNoInteractions(servicio);
    }

    @Test
    void actualizarDevuelveConflictoSiLaRutaTieneVuelos() throws Exception {
        RutaSolicitud solicitud = new RutaSolicitud(
                1L, 3L, 700, 90, true);

        when(servicio.actualizar(10L, solicitud))
                .thenThrow(
                        new ConflictoException(
                                "No se puede cambiar el recorrido "
                                        + "de una ruta con vuelos asociados")
                                .conCodigo("RUTA_CON_VUELOS_ASOCIADOS"));

        mockMvc.perform(put("/api/v1/admin/rutas/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "origenId": 1,
                            "destinoId": 3,
                            "distanciaKm": 700,
                            "duracionMin": 90,
                            "activa": true
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code")
                        .value("RUTA_CON_VUELOS_ASOCIADOS"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/admin/rutas/10"));

        verify(servicio).actualizar(10L, solicitud);
    }
}