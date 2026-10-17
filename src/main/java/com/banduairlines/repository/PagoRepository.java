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

    @Query("SELECT p FROM Pago p WHERE p.reserva.usuario.id = :usuarioId AND p.estado = 'PAGADO' ORDER BY p.fechaPago DESC, p.createdAt DESC")
    Page<Pago> findPagadosByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

<<<<<<< Updated upstream
    @Query("SELECT p FROM Pago p WHERE (:desde IS NULL OR p.fechaPago >= :desde) AND (:hasta IS NULL OR p.fechaPago <= :hasta)")
    List<Pago> findByRangoFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT p FROM Pago p WHERE (:desde IS NULL OR p.fechaPago >= :desde) AND (:hasta IS NULL OR p.fechaPago <= :hasta)")
    Page<Pago> findByRangoFechasPaginado(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);
=======
    /** Sin parámetros nulos: el servicio usa límites amplios y '%' cuando el filtro no viene. */
    @Query("SELECT p FROM Pago p WHERE p.reserva.usuario.id = :usuarioId AND p.estado = 'PAGADO' " +
            "AND p.fechaPago >= :desde AND p.fechaPago <= :hasta " +
            "AND UPPER(p.reserva.codigoReserva) LIKE :codigoReserva " +
            "ORDER BY p.fechaPago DESC, p.createdAt DESC")
    Page<Pago> findPagadosByUsuarioId(@Param("usuarioId") Long usuarioId,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      @Param("codigoReserva") String codigoReserva,
                                      Pageable pageable);

    @Query("SELECT p FROM Pago p WHERE p.estado = 'PAGADO' AND p.fechaPago >= :desde AND p.fechaPago <= :hasta")
    List<Pago> findPagadosEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(p) FROM Pago p WHERE p.estado = 'RECHAZADO' AND p.createdAt >= :desde AND p.createdAt <= :hasta")
    long countRechazadosEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
>>>>>>> Stashed changes
}
