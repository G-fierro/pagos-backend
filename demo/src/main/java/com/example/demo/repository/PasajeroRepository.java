package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long>{
    
}
