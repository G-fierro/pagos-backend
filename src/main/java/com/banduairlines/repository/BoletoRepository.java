package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{
    
}
