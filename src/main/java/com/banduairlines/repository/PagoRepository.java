package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    
}
