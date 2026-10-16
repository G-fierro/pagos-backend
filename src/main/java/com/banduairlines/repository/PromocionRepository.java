package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    @Query("""
            select promocion
            from Promocion promocion
            where (:activa is null or promocion.activa = :activa)
            order by promocion.codigo
            """)
    List<Promocion> listarAdministracion(@Param("activa") Boolean activa);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
