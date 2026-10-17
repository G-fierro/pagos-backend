package com.banduairlines.service.pagos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.banduairlines.dto.pagos.FlowPaymentCreateResponse;
import com.banduairlines.dto.pagos.FlowPaymentStatusResponse;
import com.banduairlines.dto.pagos.FlowRefundResponse;
import com.banduairlines.exception.ServicioExternoException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FlowClientImpl implements FlowClient {

    private static final Logger log = LoggerFactory.getLogger(FlowClientImpl.class);

    public static final String DEFAULT_API_KEY = "2CFCEE34-2575-4EC9-ACA2-65A72DLDDF2D";
    public static final String DEFAULT_SECRET_KEY = "5c001b7af4386a9c6b927fe37f7996b818625148";
    public static final String DEFAULT_API_URL = "https://sandbox.flow.cl/api";

    private final String apiKey;
    private final String secretKey;
    private final String apiUrl;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public FlowClientImpl(
            @Value("${bandu.flow.api-key:}") String apiKey,
            @Value("${bandu.flow.secret-key:}") String secretKey,
            @Value("${bandu.flow.api-url:https://sandbox.flow.cl/api}") String apiUrl
    ) {
        this(
            (apiKey != null && !apiKey.isBlank()) ? apiKey : DEFAULT_API_KEY,
            (secretKey != null && !secretKey.isBlank()) ? secretKey : DEFAULT_SECRET_KEY,
            (apiUrl != null && !apiUrl.isBlank()) ? apiUrl : DEFAULT_API_URL,
            RestClient.builder()
        );
    }

    FlowClientImpl(String apiKey, String secretKey, String apiUrl, RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.restClient = restClientBuilder.baseUrl(this.apiUrl).build();
    }

    @Override
    public FlowPaymentCreateResponse crearPago(
            String commerceOrder,
            String subject,
            Integer amount,
            String email,
            String urlConfirmation,
            String urlReturn,
            Long timeoutSeconds
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", apiKey);
        params.put("commerceOrder", commerceOrder);
        params.put("subject", subject);
        params.put("currency", "CLP");
        params.put("amount", String.valueOf(amount));
        params.put("email", email);
        params.put("urlConfirmation", urlConfirmation);
        params.put("urlReturn", urlReturn);
        if (timeoutSeconds != null && timeoutSeconds > 0) {
            params.put("timeout", String.valueOf(timeoutSeconds));
        }

        String signature = firmar(params, secretKey);
        params.put("s", signature);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        params.forEach(formData::add);

        try {
            log.info("Creando pago en Flow para orden: {}, monto: {}", commerceOrder, amount);
            return restClient.post()
                    .uri("/payment/create")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(FlowPaymentCreateResponse.class);
        } catch (Exception e) {
            log.error("Error al llamar a Flow payment/create para orden {}: {}", commerceOrder, e.getMessage(), e);
            throw new ServicioExternoException("Flow no respondió o rechazó la solicitud de pago", e)
                    .conCodigo("SERVICIO_EXTERNO");
        }
    }

    @Override
    public FlowPaymentStatusResponse obtenerEstadoPago(String token) {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", apiKey);
        params.put("token", token);

        String signature = firmar(params, secretKey);

        try {
            log.info("Consultando estado de pago en Flow para token: {}", token);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payment/getStatus")
                            .queryParam("apiKey", apiKey)
                            .queryParam("token", token)
                            .queryParam("s", signature)
                            .build())
                    .retrieve()
                    .body(FlowPaymentStatusResponse.class);
        } catch (Exception e) {
            log.error("Error al consultar estado de pago en Flow (token={}): {}", token, e.getMessage(), e);
            throw new ServicioExternoException("Flow no respondió al consultar el estado del pago", e)
                    .conCodigo("SERVICIO_EXTERNO");
        }
    }

    @Override
    public FlowRefundResponse crearReembolso(
            String refundCommerceOrder,
            String receiverEmail,
            Integer amount,
            String urlCallBack,
            Long flowTrxId
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", apiKey);
        params.put("refundCommerceOrder", refundCommerceOrder);
        params.put("receiverEmail", receiverEmail);
        params.put("amount", String.valueOf(amount));
        params.put("urlCallBack", urlCallBack);
        params.put("flowTrxId", String.valueOf(flowTrxId));

        String signature = firmar(params, secretKey);
        params.put("s", signature);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        params.forEach(formData::add);

        try {
            log.info("Solicitando reembolso a Flow: refundCommerceOrder={}, flowTrxId={}, amount={}",
                    refundCommerceOrder, flowTrxId, amount);
            return restClient.post()
                    .uri("/refund/create")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(FlowRefundResponse.class);
        } catch (Exception e) {
            log.error("Error al crear reembolso en Flow: {}", e.getMessage(), e);
            throw new ServicioExternoException("Error al comunicarse con Flow para procesar reembolso", e)
                    .conCodigo("SERVICIO_EXTERNO");
        }
    }

    @Override
    public FlowRefundResponse obtenerEstadoReembolso(String token) {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", apiKey);
        params.put("token", token);

        String signature = firmar(params, secretKey);

        try {
            log.info("Consultando estado de reembolso en Flow para token: {}", token);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/refund/getStatus")
                            .queryParam("apiKey", apiKey)
                            .queryParam("token", token)
                            .queryParam("s", signature)
                            .build())
                    .retrieve()
                    .body(FlowRefundResponse.class);
        } catch (Exception e) {
            log.error("Error al consultar reembolso en Flow (token={}): {}", token, e.getMessage(), e);
            throw new ServicioExternoException("Flow no respondió al consultar estado del reembolso", e)
                    .conCodigo("SERVICIO_EXTERNO");
        }
    }

    public static String firmar(Map<String, String> params, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return "dummy-signature";
        }
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder toSign = new StringBuilder();
        for (String key : keys) {
            toSign.append(key).append(params.get(key));
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(toSign.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al firmar mensaje para Flow", e);
        }
    }
}
