package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.TarifaVuelo;

public interface TarifaVueloRepository extends JpaRepository<TarifaVuelo, Long>{
    
}
