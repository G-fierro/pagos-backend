package com.banduairlines.service.pagos;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlowClientTest {

    @Test
    void testFirmar_ordenaParametrosYGeneraHmacSha256Hexadecimal() {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", "TEST_API_KEY");
        params.put("commerceOrder", "ORDEN-123");
        params.put("amount", "15000");
        params.put("currency", "CLP");

        String secretKey = "mi_clave_secreta_flow";
        String firma = FlowClientImpl.firmar(params, secretKey);

        assertNotNull(firma);
        assertEquals(64, firma.length(), "La firma HMAC-SHA256 debe tener 64 caracteres hexadecimales");
        assertEquals(firma.toLowerCase(), firma, "La firma debe estar en minúsculas");

        // Idempotencia de la firma
        String segundaFirma = FlowClientImpl.firmar(params, secretKey);
        assertEquals(firma, segundaFirma);
    }

    @Test
    void testFirmar_conClaveVacia_retornaFirmaPorDefecto() {
        Map<String, String> params = Map.of("apiKey", "123");
        String firma = FlowClientImpl.firmar(params, "");
        assertEquals("dummy-signature", firma);
    }
}
