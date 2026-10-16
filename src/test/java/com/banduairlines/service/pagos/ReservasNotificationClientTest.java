package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.ResultadoPagoRequest;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.model.enums.EstadoPago;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ReservasNotificationClientTest {

    private final ResultadoPagoRequest request = new ResultadoPagoRequest(
            10L, 5555L, EstadoPago.PAGADO, 255320, Instant.now(), null);

    private ReservasNotificationClient crearCliente(RestClient.Builder builder) {
        return new ReservasNotificationClient("clave", "http://reservas.test", builder);
    }

    @Test
    void respuesta2xx_esExitosa() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://reservas.test/api/v1/internas/reservas/1/resultado-pago"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Service-Key", "clave"))
                .andRespond(withSuccess());

        assertTrue(crearCliente(builder).notificarResultado(1L, request).exitoso());
        server.verify();
    }

    @Test
    void respuesta409_seInformaConSuCodigoSinLanzar() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://reservas.test/api/v1/internas/reservas/1/resultado-pago"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"PAGO_YA_PAGADO\"}"));

        ReservasNotificationClient.ReservasNotificacionResult res = crearCliente(builder).notificarResultado(1L, request);

        assertFalse(res.exitoso());
        assertEquals("PAGO_YA_PAGADO", res.errorCode());
    }

    @Test
    void respuesta5xx_lanzaServicioExterno() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://reservas.test/api/v1/internas/reservas/1/resultado-pago"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(ServicioExternoException.class, () -> crearCliente(builder).notificarResultado(1L, request));
    }

    @Test
    void sinConexion_lanzaServicioExternoEnVezDeSimularExito() {
        ReservasNotificationClient cliente =
                new ReservasNotificationClient("clave", "http://localhost:1", RestClient.builder());

        assertThrows(ServicioExternoException.class, () -> cliente.notificarResultado(1L, request));
    }
}
