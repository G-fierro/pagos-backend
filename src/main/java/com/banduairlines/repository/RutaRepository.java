package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.banduairlines.model.Ruta;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {
    List<Ruta> findByActivaTrue();
}