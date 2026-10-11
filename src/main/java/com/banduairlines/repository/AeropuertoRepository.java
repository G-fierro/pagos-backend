package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Aeropuerto;

public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {

    boolean existsByCodigoIata(String codigoIata);
}
