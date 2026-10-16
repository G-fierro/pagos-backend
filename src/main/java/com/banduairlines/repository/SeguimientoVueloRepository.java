package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.banduairlines.model.SeguimientoVuelo;

import java.util.Optional;

public interface SeguimientoVueloRepository extends JpaRepository<SeguimientoVuelo, Long> {

    Optional<SeguimientoVuelo> findFirstByVueloIdOrderByRegistradoEnDesc(Long vueloId);
}