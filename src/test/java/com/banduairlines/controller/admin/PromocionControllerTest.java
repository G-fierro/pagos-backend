package com.banduairlines.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.service.admin.PromocionService;

@ExtendWith(MockitoExtension.class)
class PromocionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PromocionService promocionService;

    @InjectMocks
    private PromocionController promocionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(promocionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listar_retorna200() throws Exception {
        when(promocionService.listar(null)).thenReturn(List.of(
                new PromocionRespuesta(1L, "DESC10", TipoDescuento.PORCENTAJE, new BigDecimal("10.00"),
                        LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(10), null, 0, true)
        ));

        mockMvc.perform(get("/api/v1/admin/promociones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("DESC10"))
                .andExpect(jsonPath("$[0].tipoDescuento").value("PORCENTAJE"))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    @Test
    void crear_cuandoValido_retorna201() throws Exception {
        when(promocionService.crear(any(PromocionSolicitud.class)))
                .thenReturn(new PromocionRespuesta(1L, "DESC10", TipoDescuento.PORCENTAJE, new BigDecimal("10.00"),
                        LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(10), null, 0, true));

        String json = """
                {
                  "codigo": "DESC10",
                  "tipoDescuento": "PORCENTAJE",
                  "valor": 10,
                  "vigenteDesde": "2026-12-01T00:00:00",
                  "vigenteHasta": "2026-12-31T23:59:59"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("DESC10"))
                .andExpect(jsonPath("$.tipoDescuento").value("PORCENTAJE"));
    }

    @Test
    void crear_cuandoDatoInvalido_retorna400() throws Exception {
        String json = """
                { "codigo": "", "tipoDescuento": "PORCENTAJE", "valor": -1, "vigenteDesde": "2026-12-01T00:00:00", "vigenteHasta": "2026-12-31T23:59:59" }
                """;

        mockMvc.perform(post("/api/v1/admin/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
