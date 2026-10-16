package com.banduairlines.repository;

import java.time.LocalDateTime;
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

    /**
     * Asientos de un vuelo tomados por otras reservas: confirmadas, o abiertas con el bloqueo vigente.
     * A diferencia de asientoOcupadoPorOtraReserva, ignora las compras vencidas que aún no cancela la tarea programada.
     */
    @Query("""
            select b.asientoId.id from Boleto b
            where b.vueloId.id = :vueloId
              and b.asientoId is not null
              and b.reserva.id <> :reservaId
              and (b.reserva.estado = :confirmada
                   or (b.reserva.estado in :abiertas and b.reserva.expiraEn > :ahora))
            """)
    List<Long> asientosTomadosPorOtras(@Param("vueloId") Long vueloId,
                                       @Param("reservaId") Long reservaId,
                                       @Param("confirmada") EstadoReserva confirmada,
                                       @Param("abiertas") Collection<EstadoReserva> abiertas,
                                       @Param("ahora") LocalDateTime ahora);
}
