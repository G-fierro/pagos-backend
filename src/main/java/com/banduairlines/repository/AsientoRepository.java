package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Asiento;

public interface AsientoRepository extends JpaRepository<Asiento, Long>{
    
}
