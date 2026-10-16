package com.banduairlines.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.enums.EstadoReserva;

/**
 * ddl-auto=update crea tablas y columnas nuevas, pero no modifica las restricciones de las que ya existen.
 * Este ajuste deja la tabla reservas al día en las bases creadas antes de los endpoints 7-12 de RESERVA:
 * - el CHECK de estado debe admitir todos los valores de EstadoReserva (EN_CURSO es nuevo);
 * - codigo_reserva admite null, porque el código se asigna al iniciar el pago (ReservaPagoServiceImpl).
 * En una base que ya está al día no hace nada. Se puede borrar cuando todas las bases (locales y servidor)
 * hayan arrancado al menos una vez con esta versión.
 */
@Component
public class AjusteEsquemaReservas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteEsquemaReservas.class);

    private static final String CHECK_ESTADO = "reservas_estado_check";

    private final JdbcTemplate jdbcTemplate;

    public AjusteEsquemaReservas(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ajustarCheckDeEstado();
        permitirCodigoReservaNulo();
        permitirEmailContactoNulo();
    }

    /** El correo llega en PUT /contacto (R-02); se exige al pagar, no al crear la reserva (endpoint 1). */
    private void permitirEmailContactoNulo() {
        Boolean obligatorio = jdbcTemplate.queryForObject(
                "SELECT is_nullable = 'NO' FROM information_schema.columns"
                        + " WHERE table_schema = current_schema() AND table_name = 'reservas' AND column_name = 'email_contacto'",
                Boolean.class);
        if (Boolean.TRUE.equals(obligatorio)) {
            jdbcTemplate.execute("ALTER TABLE reservas ALTER COLUMN email_contacto DROP NOT NULL");
            log.info("Ajuste de esquema: reservas.email_contacto ahora admite null");
        }
    }

    private void ajustarCheckDeEstado() {
        List<String> definicion = jdbcTemplate.queryForList(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'reservas'::regclass AND conname = ?",
                String.class, CHECK_ESTADO);
        List<String> valores = Arrays.stream(EstadoReserva.values()).map(e -> "'" + e.name() + "'").toList();
        if (!definicion.isEmpty() && valores.stream().allMatch(definicion.get(0)::contains)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE reservas DROP CONSTRAINT IF EXISTS " + CHECK_ESTADO);
        jdbcTemplate.execute("ALTER TABLE reservas ADD CONSTRAINT " + CHECK_ESTADO
                + " CHECK (estado IN (" + String.join(", ", valores) + "))");
        log.info("Ajuste de esquema: {} ahora admite {}", CHECK_ESTADO, valores);
    }

    private void permitirCodigoReservaNulo() {
        Boolean obligatorio = jdbcTemplate.queryForObject(
                "SELECT is_nullable = 'NO' FROM information_schema.columns"
                        + " WHERE table_schema = current_schema() AND table_name = 'reservas' AND column_name = 'codigo_reserva'",
                Boolean.class);
        if (Boolean.TRUE.equals(obligatorio)) {
            jdbcTemplate.execute("ALTER TABLE reservas ALTER COLUMN codigo_reserva DROP NOT NULL");
            log.info("Ajuste de esquema: reservas.codigo_reserva ahora admite null");
        }
    }
}
