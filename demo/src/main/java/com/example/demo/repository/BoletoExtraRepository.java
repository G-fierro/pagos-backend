package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.BoletoExtra;
import com.example.demo.model.BoletoExtraId;

public interface BoletoExtraRepository extends JpaRepository<BoletoExtra, BoletoExtraId>{
    
}
