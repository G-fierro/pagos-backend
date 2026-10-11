package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>{
    
}
