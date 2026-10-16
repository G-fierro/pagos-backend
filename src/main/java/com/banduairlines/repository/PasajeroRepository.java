package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long>{

    long countByReserva_Id(Long reservaId);

    List<Pasajero> findByReserva_IdOrderByIdAsc(Long reservaId);
}
