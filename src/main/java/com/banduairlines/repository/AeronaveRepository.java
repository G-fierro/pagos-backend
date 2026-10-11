package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Avion;

public interface AeronaveRepository extends JpaRepository<Avion, Long>{
    
}
