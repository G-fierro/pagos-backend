package com.banduairlines.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{

    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);
}
