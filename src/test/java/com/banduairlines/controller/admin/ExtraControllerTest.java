package com.banduairlines.controller.admin;

import java.math.BigDecimal;
import java.util.List;

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

import com.banduairlines.dto.admin.ExtraRespuesta;
import com.banduairlines.dto.admin.ExtraSolicitud;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.service.admin.ExtraService;

@ExtendWith(MockitoExtension.class)
class ExtraControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExtraService extraService;

    @InjectMocks
    private ExtraController extraController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(extraController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listar_retorna200() throws Exception {
        when(extraService.listar(null)).thenReturn(List.of(
                new ExtraRespuesta(1L, TipoExtra.ASIENTO_PREFERENTE, "Asiento Preferente", null, new BigDecimal("8000.00"), true)
        ));

        mockMvc.perform(get("/api/v1/admin/extras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ASIENTO_PREFERENTE"))
                .andExpect(jsonPath("$[0].nombre").value("Asiento Preferente"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void crear_cuandoValido_retorna201() throws Exception {
        when(extraService.crear(any(ExtraSolicitud.class)))
                .thenReturn(new ExtraRespuesta(1L, TipoExtra.EMBARQUE_PRIORITARIO, "Embarque Prioritario", null, new BigDecimal("5000.00"), true));

        String json = """
                { "tipo": "EMBARQUE_PRIORITARIO", "nombre": "Embarque Prioritario", "precio": 5000 }
                """;

        mockMvc.perform(post("/api/v1/admin/extras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("EMBARQUE_PRIORITARIO"))
                .andExpect(jsonPath("$.nombre").value("Embarque Prioritario"));
    }

    @Test
    void crear_cuandoDatoInvalido_retorna400() throws Exception {
        String json = """
                { "tipo": "EMBARQUE_PRIORITARIO", "nombre": "", "precio": -1 }
                """;

        mockMvc.perform(post("/api/v1/admin/extras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
