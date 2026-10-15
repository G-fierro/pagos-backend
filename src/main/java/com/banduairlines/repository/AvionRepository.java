package com.banduairlines.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.banduairlines.model.Avion;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvionRepository extends JpaRepository<Avion, Long> {

    // Opcional pero muy útil para buscar aeronaves por su matrícula única
    Optional<Avion> findByMatricula(String matricula);
}
