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
}