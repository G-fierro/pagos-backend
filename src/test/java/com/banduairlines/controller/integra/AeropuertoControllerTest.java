package com.banduairlines.controller.integra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.integra.AeropuertoRequest;
import com.banduairlines.dto.integra.AeropuertoResponse;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.service.AeropuertoService;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
@ExtendWith(MockitoExtension.class)
class AeropuertoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AeropuertoService aeropuertoService;

    @InjectMocks
    private AeropuertoController aeropuertoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aeropuertoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void obtenerPorId_cuandoExiste_retorna200() throws Exception {
        AeropuertoResponse response = new AeropuertoResponse(
                1L, "SCL", "Arturo Merino Benítez", "Santiago", "Chile", "America/Santiago"
        );
        when(aeropuertoService.obtenerPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ejemplo/aeropuertos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.codigoIata").value("SCL"));
    }

    @Test
    void obtenerPorId_cuandoNoExiste_retorna404() throws Exception {
        when(aeropuertoService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Aeropuerto", 99L));

        mockMvc.perform(get("/api/v1/ejemplo/aeropuertos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CodigosError.RECURSO_NO_ENCONTRADO))
                .andExpect(jsonPath("$.message").value("Aeropuerto no encontrado: 99"));
    }

    @Test
    void crear_cuandoEsValido_retorna201() throws Exception {
        AeropuertoResponse response = new AeropuertoResponse(
                1L, "SCL", "Arturo Merino Benítez", "Santiago", "Chile", "America/Santiago"
        );
        when(aeropuertoService.crear(any(AeropuertoRequest.class))).thenReturn(response);

        String json = """
                {
                    "codigoIata": "SCL",
                    "nombre": "Arturo Merino Benítez",
                    "ciudad": "Santiago",
                    "pais": "Chile",
                    "zonaHoraria": "America/Santiago"
                }
                """;

        mockMvc.perform(post("/api/v1/ejemplo/aeropuertos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.codigoIata").value("SCL"));
    }

    @Test
    void crear_cuandoCodigoYaExiste_retorna409() throws Exception {
        when(aeropuertoService.crear(any(AeropuertoRequest.class)))
                .thenThrow(new ConflictoException("Ya existe un aeropuerto con el código IATA: SCL"));

        String json = """
                {
                    "codigoIata": "SCL",
                    "nombre": "Arturo Merino Benítez",
                    "ciudad": "Santiago",
                    "pais": "Chile",
                    "zonaHoraria": "America/Santiago"
                }
                """;

        mockMvc.perform(post("/api/v1/ejemplo/aeropuertos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CodigosError.CONFLICTO))
                .andExpect(jsonPath("$.message").value("Ya existe un aeropuerto con el código IATA: SCL"));
    }

    @Test
    void crear_cuandoDatoInvalido_retorna400() throws Exception {
        String json = """
                {
                    "codigoIata": "invalido",
                    "nombre": "",
                    "ciudad": ""
                }
                """;

        mockMvc.perform(post("/api/v1/ejemplo/aeropuertos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CodigosError.VALIDACION))
                .andExpect(jsonPath("$.details[?(@.campo == 'codigoIata')]").exists());
    }
}
