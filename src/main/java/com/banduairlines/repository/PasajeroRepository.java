package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long>{
    
}
