package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long>{
    
}
