package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Extra;

public interface ExtraRepository extends JpaRepository<Extra, Long> {

    @Query("""
            select extra
            from Extra extra
            where (:activo is null or extra.activo = :activo)
            order by extra.tipo
            """)
    List<Extra> listarAdministracion(@Param("activo") Boolean activo);
}
