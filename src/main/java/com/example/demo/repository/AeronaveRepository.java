package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Avion;

public interface AeronaveRepository extends JpaRepository<Avion, Long>{
    
}
