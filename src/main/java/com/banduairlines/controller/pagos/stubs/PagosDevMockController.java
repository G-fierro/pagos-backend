package com.banduairlines.controller.pagos.stubs;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.pagos.stubs.DevJwtAuthHelper;
import com.banduairlines.service.pagos.stubs.DevReservaMockService;

/**
 * Controlador de utilidades y endpoints de desarrollo para pruebas del módulo de pagos.
 * Facilita sembrar reservas con vigencia fresca, resetear estados para repetir flujos
 * en Postman y emitir tokens JWT válidos sin pasar por el módulo de CUENTA.
 */
@RestController
@RequestMapping("/api/v1/pagos/dev")
public class PagosDevMockController {

    private final DevReservaMockService devReservaMockService;
    private final DevJwtAuthHelper devJwtAuthHelper;
    private final ReservaRepository reservaRepository;

    public PagosDevMockController(
            DevReservaMockService devReservaMockService,
            DevJwtAuthHelper devJwtAuthHelper,
            ReservaRepository reservaRepository
    ) {
        this.devReservaMockService = devReservaMockService;
        this.devJwtAuthHelper = devJwtAuthHelper;
        this.reservaRepository = reservaRepository;
    }

    @PostMapping("/reservas/sembrar")
    public ResponseEntity<Map<String, Object>> sembrarReservas() {
        devReservaMockService.sembrarTodasLasReservas();
        return ResponseEntity.ok(Map.of(
                "mensaje", "Reservas de prueba sembradas exitosamente",
                "reservas", List.of("1 (K7QX2M - PENDIENTE_PAGO)", "VENC99 (Expirada)", "CONF88 (Confirmada)", "GUEST7 (Invitado)")
        ));
    }

    @PostMapping("/reservas/{reservaId}/reset")
    public ResponseEntity<Map<String, Object>> resetearReserva(@PathVariable Long reservaId) {
        Reserva r = devReservaMockService.restablecerReserva(reservaId);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Reserva restablecida a PENDIENTE_PAGO con vigencia renovada y pagos previos eliminados",
                "reservaId", r.getId(),
                "codigoReserva", r.getCodigoReserva(),
                "estado", r.getEstado().name(),
                "expiraEn", r.getExpiraEn()
        ));
    }

    @RequestMapping(value = "/auth/token", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> obtenerTokenDev(
            @RequestParam(defaultValue = "PASAJERO") String rol,
            @RequestParam(defaultValue = "1") Long usuarioId,
            @RequestParam(defaultValue = "javiera.munoz@gmail.com") String email
    ) {
        RolUsuario rolUsuario = RolUsuario.PASAJERO;
        try {
            rolUsuario = RolUsuario.valueOf(rol.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        String token = devJwtAuthHelper.generarToken(usuarioId, email, rolUsuario);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenTipo", "Bearer",
                "rol", rolUsuario.name(),
                "usuarioId", usuarioId,
                "email", email,
                "headerEjemplo", "Authorization: Bearer " + token
        ));
    }

    @GetMapping("/reservas")
    public ResponseEntity<List<Map<String, Object>>> listarReservasEstado() {
        List<Map<String, Object>> lista = reservaRepository.findAll().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "codigo", r.getCodigoReserva(),
                        "estado", r.getEstado().name(),
                        "totalCLP", r.getTotal() != null ? r.getTotal().intValue() : 0,
                        "expiraEn", r.getExpiraEn() != null ? r.getExpiraEn().toString() : "null"
                ))
                .toList();
        return ResponseEntity.ok(lista);
    }
}
