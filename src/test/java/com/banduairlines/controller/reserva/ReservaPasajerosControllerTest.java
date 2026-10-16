package com.banduairlines.controller.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.dto.reserva.PasajeroResponse;
import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.GlobalExceptionHandler;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.service.reserva.ReservaContactoService;
import com.banduairlines.service.reserva.ReservaPasajerosService;

@ExtendWith(MockitoExtension.class)
class ReservaPasajerosControllerTest {

    private static final String PASAJEROS_VALIDOS = """
            {"pasajeros":[{"ref":"a1","tipo":"ADULTO","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"1990-03-15",
              "tipoDocumento":"RUT","numeroDocumento":"11111111-1","nacionalidad":"Chilena"}]}
            """;

    @Mock private ReservaPasajerosService reservaPasajerosService;
    @Mock private ReservaContactoService reservaContactoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaPasajerosController(reservaPasajerosService, reservaContactoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------- endpoint 3

    @Test
    void registrarPasajeros_valido_responde200ConPasajeroId() throws Exception {
        when(reservaPasajerosService.registrar(eq(1L), eq("tok"), any(RegistrarPasajerosRequest.class)))
                .thenReturn(new PasajerosRegistradosResponse(List.of(new PasajeroResponse(7L, "a1", TipoPasajero.ADULTO,
                        "Ana", "Pérez", LocalDate.of(1990, 3, 15), TipoDocumento.RUT, "11111111-1", "Chilena", null))));

        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS_VALIDOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasajeros[0].pasajeroId").value(7))
                .andExpect(jsonPath("$.pasajeros[0].ref").value("a1"));
    }

    @Test
    void registrarPasajeros_sinNacionalidad_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pasajeros":[{"ref":"a1","tipo":"ADULTO","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"1990-03-15",
                          "tipoDocumento":"RUT","numeroDocumento":"11111111-1"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.details[0].campo").value("pasajeros[0].nacionalidad"));
        verifyNoInteractions(reservaPasajerosService);
    }

    @Test
    void registrarPasajeros_fechaNacimientoFutura_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pasajeros":[{"ref":"a1","tipo":"ADULTO","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"2999-01-01",
                          "tipoDocumento":"RUT","numeroDocumento":"11111111-1","nacionalidad":"Chilena"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void registrarPasajeros_documentoRepetido_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pasajeros":[
                          {"ref":"a1","tipo":"ADULTO","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"1990-03-15",
                           "tipoDocumento":"RUT","numeroDocumento":"11111111-1","nacionalidad":"Chilena"},
                          {"ref":"a2","tipo":"ADULTO","nombres":"Luis","apellidos":"Pérez","fechaNacimiento":"1988-03-15",
                           "tipoDocumento":"RUT","numeroDocumento":"11111111-1","nacionalidad":"Chilena"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void registrarPasajeros_tipoInexistente_responde400PeticionMalformada() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").contentType(MediaType.APPLICATION_JSON)
                        .content(PASAJEROS_VALIDOS.replace("\"ADULTO\"", "\"ANCIANO\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_MALFORMADA"));
    }

    @Test
    void registrarPasajeros_reglaDeNegocio_responde422ConSuCodigo() throws Exception {
        when(reservaPasajerosService.registrar(eq(1L), eq("tok"), any(RegistrarPasajerosRequest.class)))
                .thenThrow(new ReglaNegocioException("El tipo no coincide").conCodigo("TIPO_NO_COINCIDE_EDAD"));

        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS_VALIDOS))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("TIPO_NO_COINCIDE_EDAD"));
    }

    @Test
    void registrarPasajeros_reservaVencida_responde409ReservaExpirada() throws Exception {
        when(reservaPasajerosService.registrar(eq(1L), eq("tok"), any(RegistrarPasajerosRequest.class)))
                .thenThrow(new ConflictoException("Venció el tiempo").conCodigo("RESERVA_EXPIRADA"));

        mockMvc.perform(put("/api/v1/reservas/1/pasajeros").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(PASAJEROS_VALIDOS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVA_EXPIRADA"));
    }

    // ---------------------------------------------------------------- endpoint 4

    @Test
    void registrarContacto_valido_responde200() throws Exception {
        when(reservaContactoService.registrar(eq(1L), eq("tok"), any(ContactoRequest.class)))
                .thenReturn(new ContactoResponse("Tomás Rojas", "tomas@correo.cl", "+56912345678"));

        mockMvc.perform(put("/api/v1/reservas/1/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Tomás Rojas\",\"correo\":\"tomas@correo.cl\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("tomas@correo.cl"));
    }

    @Test
    void registrarContacto_correoInvalido_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Tomás\",\"correo\":\"no-es-correo\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.details[0].campo").value("correo"));
        verifyNoInteractions(reservaContactoService);
    }

    @Test
    void registrarContacto_sinCorreoNiTelefono_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Tomás\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void registrarContacto_telefonoConLetras_responde400Validacion() throws Exception {
        mockMvc.perform(put("/api/v1/reservas/1/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Tomás\",\"correo\":\"t@correo.cl\",\"telefono\":\"llamame\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].campo").value("telefono"));
    }

    @Test
    void registrarContacto_reservaYaEnPago_responde409EstadoInvalido() throws Exception {
        when(reservaContactoService.registrar(eq(1L), eq("tok"), any(ContactoRequest.class)))
                .thenThrow(new ConflictoException("La reserva está PENDIENTE_PAGO").conCodigo("ESTADO_INVALIDO"));

        mockMvc.perform(put("/api/v1/reservas/1/contacto").header("X-Compra-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Tomás\",\"correo\":\"t@correo.cl\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ESTADO_INVALIDO"));
    }
}
