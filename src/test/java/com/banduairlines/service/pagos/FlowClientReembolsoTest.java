package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.FlowRefundResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Flow entrega los montos del reembolso como texto decimal ("12990.00"). Con amount como Integer,
 * leer la respuesta fallaba y Pagos perdía el token del reembolso aunque Flow lo hubiera creado.
 */
class FlowClientReembolsoTest {

    private static final String RESPUESTA = """
            {"token":"REF_TOKEN_9","flowRefundOrder":4455,"date":"2026-10-17 00:59:27",
             "status":"created","amount":"12990.00","fee":"0.00"}""";

    @Test
    void crearReembolso_leeElMontoDecimalEnTextoYConservaElToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://sandbox.flow.cl/api/refund/create"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
        FlowClientImpl cliente = new FlowClientImpl("key", "secret", "https://sandbox.flow.cl/api", builder);

        FlowRefundResponse respuesta = cliente.crearReembolso("REF-9", "a@ufromail.cl", 12990, "http://callback", 11108299L);

        assertEquals("REF_TOKEN_9", respuesta.token());
        assertEquals(4455L, respuesta.flowRefundOrder());
        assertEquals("created", respuesta.status());
        assertEquals(0, new BigDecimal("12990.00").compareTo(respuesta.amount()));
        server.verify();
    }

    @Test
    void obtenerEstadoReembolso_leeElMontoDecimalEnTexto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA.replace("created", "accepted"), MediaType.APPLICATION_JSON));
        FlowClientImpl cliente = new FlowClientImpl("key", "secret", "https://sandbox.flow.cl/api", builder);

        FlowRefundResponse respuesta = cliente.obtenerEstadoReembolso("REF_TOKEN_9");

        assertEquals("accepted", respuesta.status());
        assertEquals(0, new BigDecimal("12990.00").compareTo(respuesta.amount()));
    }
}
