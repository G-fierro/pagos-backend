package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{
    
}
