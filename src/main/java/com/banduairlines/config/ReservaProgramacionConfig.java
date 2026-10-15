package com.banduairlines.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas. La usa RESERVA para cancelar las compras cuyo bloqueo de asientos
 * venció (ReservaCancelacionServiceImpl#cancelarExpiradas).
 */
@Configuration
@EnableScheduling
public class ReservaProgramacionConfig {
}
