package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Vuelo;

public interface VueloRepository extends JpaRepository<Vuelo, Long>{
    
}
