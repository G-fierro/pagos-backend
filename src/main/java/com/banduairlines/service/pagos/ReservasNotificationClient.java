package com.banduairlines.service.pagos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.banduairlines.dto.pagos.ResultadoPagoRequest;
import com.banduairlines.exception.ServicioExternoException;

@Service
public class ReservasNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(ReservasNotificationClient.class);

    private final RestClient restClient;
    private final String serviceKey;
    private final String baseUrl;

    public static final String DEFAULT_SERVICE_KEY = "clave_compartida_reservas";
    public static final String DEFAULT_BASE_URL = "http://localhost:8080";

    @Autowired
    public ReservasNotificationClient(
            @Value("${bandu.internas.service-key:clave_compartida_reservas}") String serviceKey,
            @Value("${bandu.reservas.url:http://localhost:8080}") String baseUrl
    ) {
        this(
            (serviceKey != null && !serviceKey.isBlank()) ? serviceKey : DEFAULT_SERVICE_KEY,
            (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_BASE_URL,
            RestClient.builder()
        );
    }

    ReservasNotificationClient(String serviceKey, String baseUrl, RestClient.Builder restClientBuilder) {
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restClient = restClientBuilder.baseUrl(this.baseUrl).build();
    }

    public record ReservasNotificacionResult(boolean exitoso, int statusCode, String errorCode, String mensaje) {}

    public ReservasNotificacionResult notificarResultado(Long reservaId, ResultadoPagoRequest request) {
        try {
            log.info("Notificando resultado de pago a Reservas para reservaId={}: estado={}", reservaId, request.estado());
            restClient.post()
                    .uri("/api/v1/internas/reservas/{reservaId}/resultado-pago", reservaId)
                    .header("X-Service-Key", serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            return new ReservasNotificacionResult(true, 200, null, "Notificación exitosa");
        } catch (HttpClientErrorException e) {
            log.warn("Reservas respondió con error de cliente {} al notificar resultado: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            String errorCode = extraerCodigoError(e.getResponseBodyAsString(), e.getStatusCode());
            return new ReservasNotificacionResult(false, e.getStatusCode().value(), errorCode, e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("Reservas respondió 5xx ({}) al notificar resultado: {}", e.getStatusCode(), e.getMessage());
            throw new ServicioExternoException("Reservas falló al recibir la confirmación de pago", e)
                    .conCodigo("SERVICIO_EXTERNO");
        } catch (Exception e) {
            log.error("No fue posible conectar con Reservas: {}", e.getMessage());
            throw new ServicioExternoException("No fue posible comunicarse con Reservas", e)
                    .conCodigo("SERVICIO_EXTERNO");
        }
    }

    private String extraerCodigoError(String body, HttpStatusCode status) {
        if (body != null) {
            if (body.contains("ESTADO_INVALIDO")) return "ESTADO_INVALIDO";
            if (body.contains("PAGO_YA_PAGADO")) return "PAGO_YA_PAGADO";
            if (body.contains("MONTO_INVALIDO")) return "MONTO_INVALIDO";
        }
        if (status.value() == 409) return "ESTADO_INVALIDO";
        if (status.value() == 422) return "MONTO_INVALIDO";
        return "ERROR_CLIENTE_" + status.value();
    }
}
