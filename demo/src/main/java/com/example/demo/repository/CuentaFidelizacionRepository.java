package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CuentaFidelizacion;

public interface CuentaFidelizacionRepository extends JpaRepository<CuentaFidelizacion, Long>{
    
}
