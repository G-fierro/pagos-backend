package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    
}
