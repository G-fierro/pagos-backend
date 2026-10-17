package com.banduairlines.controller.pagos.stubs;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.pagos.ResultadoPagoRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.dto.pagos.ResultadoPagoRequest;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.service.pagos.stubs.DevReservaMockService;

/**
 * Mock Controller para simular el endpoint interno del módulo de Reservas:
 * POST /api/v1/internas/reservas/{reservaId}/resultado-pago
 *
 * Cuando Pagos procesa el retorno o notificación de Flow, notifica a Reservas mediante este endpoint.
 * Este stub permite que el ciclo de vida del pago se complete al 100% (confirmando la reserva en BD)
 * sin requerir que el equipo de Reservas tenga su backend levantado.
 */
@RestController
@RequestMapping("/api/v1/internas/reservas")
public class ReservasInternasMockController {

    private static final Logger log = LoggerFactory.getLogger(ReservasInternasMockController.class);

    private final DevReservaMockService devReservaMockService;
    private final String serviceKeyConfigurada;

    public ReservasInternasMockController(
            DevReservaMockService devReservaMockService,
            @Value("${bandu.internas.service-key:clave_compartida_reservas}") String serviceKeyConfigurada
    ) {
        this.devReservaMockService = devReservaMockService;
        this.serviceKeyConfigurada = serviceKeyConfigurada;
    }

    @PostMapping("/{reservaId}/resultado-pago")
    public ResponseEntity<Map<String, Object>> recibirResultadoPago(
            @PathVariable Long reservaId,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestBody ResultadoPagoRequest request
    ) {
        log.info("Stub Reservas recibido: reservaId={}, estado={}, pagoId={}", reservaId, request.estado(), request.pagoId());

        validarServiceKey(serviceKey);

        Reserva reservaActualizada;
        if (request.estado() == EstadoPago.PAGADO) {
            reservaActualizada = devReservaMockService.confirmarReserva(reservaId);
            log.info("Stub Reservas: Reserva ID {} confirmada exitosamente tras pago {}", reservaId, request.pagoId());
        } else {
            reservaActualizada = devReservaMockService.cancelarReserva(reservaId);
            log.info("Stub Reservas: Reserva ID {} cancelada por estado de pago {}", reservaId, request.estado());
        }

        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Resultado de pago procesado exitosamente por stub de Reservas",
                "reservaId", reservaId,
                "codigoReserva", reservaActualizada.getCodigoReserva(),
                "nuevoEstado", reservaActualizada.getEstado().name()
        ));
    }

    private void validarServiceKey(String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank() || !serviceKeyConfigurada.equals(serviceKey)) {
            throw new AuthenticationCredentialsNotFoundException("Clave compartida X-Service-Key inválida o ausente");
        }
    }
}
