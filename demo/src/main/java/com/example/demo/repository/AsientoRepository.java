package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Asiento;

public interface AsientoRepository extends JpaRepository<Asiento, Long>{
    
}
