package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{

    boolean existsByCodigoReserva(String codigoReserva);

    List<Reserva> findByEstadoInAndExpiraEnBefore(Collection<EstadoReserva> estados, LocalDateTime limite);
}
