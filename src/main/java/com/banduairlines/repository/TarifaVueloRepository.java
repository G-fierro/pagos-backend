package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.TarifaVuelo;

public interface TarifaVueloRepository extends JpaRepository<TarifaVuelo, Long>{
    
}
