package com.banduairlines.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Boleto;
import com.banduairlines.model.enums.EstadoReserva;

public interface BoletoRepository extends JpaRepository<Boleto, Long>{

    List<Boleto> findByReserva_IdOrderByOrdenTramoAscIdAsc(Long reservaId);

    @Query("""
            select count(b) > 0 from Boleto b
            where b.vueloId.id = :vueloId
              and b.asientoId.id = :asientoId
              and b.reserva.id <> :reservaId
              and b.reserva.estado in :estados
            """)
    boolean asientoOcupadoPorOtraReserva(@Param("vueloId") Long vueloId,
                                         @Param("asientoId") Long asientoId,
                                         @Param("reservaId") Long reservaId,
                                         @Param("estados") Collection<EstadoReserva> estados);
}
