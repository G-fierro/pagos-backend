package com.banduairlines.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long>{

    Optional<Promocion> findByCodigoIgnoreCase(String codigo);
}
