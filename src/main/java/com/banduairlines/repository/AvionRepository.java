package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Avion;

public interface AvionRepository extends JpaRepository<Avion, Long>{
    
}
