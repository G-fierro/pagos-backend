package com.banduairlines.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Pago;
import com.banduairlines.model.enums.EstadoPago;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByFlowToken(String flowToken);

    Optional<Pago> findByFlowOrder(Long flowOrder);

    List<Pago> findByReservaIdOrderByCreatedAtDesc(Long reservaId);

    Optional<Pago> findFirstByReservaIdAndEstadoOrderByIdDesc(Long reservaId, EstadoPago estado);

    /**
     * Busca el pago por el token del reembolso que Pagos guarda en
     * detalle_respuesta (PostgreSQL).
     */
    @Query(value = "SELECT * FROM pagos WHERE CAST(detalle_respuesta AS jsonb) -> 'reembolso' ->> 'token' = :token LIMIT 1", nativeQuery = true)
    Optional<Pago> findByReembolsoToken(@Param("token") String token);

    /**
     * Sin parámetros nulos: el servicio usa límites amplios y '%' cuando el filtro
     * no viene.
     */
    @Query("SELECT p FROM Pago p WHERE p.reserva.usuario.id = :usuarioId AND p.estado = 'PAGADO' " +
            "AND p.fechaPago >= :desde AND p.fechaPago <= :hasta " +
            "AND UPPER(p.reserva.codigoReserva) LIKE :codigoReserva " +
            "ORDER BY p.fechaPago DESC, p.createdAt DESC")
    Page<Pago> findPagadosByUsuarioId(@Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("codigoReserva") String codigoReserva,
            Pageable pageable);
}