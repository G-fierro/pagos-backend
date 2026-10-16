package com.banduairlines.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoReserva;

public interface TramoReservaRepository extends JpaRepository<TramoReserva, Long> {

    List<TramoReserva> findByReserva_IdOrderByOrdenAsc(Long reservaId);

    /**
     * Plazas con asiento (adultos + niños; los bebés no ocupan asiento) tomadas en un vuelo y cabina
     * por reservas confirmadas o por compras abiertas cuyo bloqueo aún no vence.
     */
    @Query("""
            select coalesce(sum(coalesce(r.cantidadAdultos, 0) + coalesce(r.cantidadNinos, 0)), 0)
            from TramoReserva t join t.reserva r
            where t.vuelo.id = :vueloId
              and t.tarifaVuelo.claseCabina = :cabina
              and (r.estado = :confirmada
                   or (r.estado in :abiertas and r.expiraEn > :ahora))
            """)
    long contarPlazasTomadas(@Param("vueloId") Long vueloId,
                             @Param("cabina") ClaseCabina cabina,
                             @Param("confirmada") EstadoReserva confirmada,
                             @Param("abiertas") Collection<EstadoReserva> abiertas,
                             @Param("ahora") LocalDateTime ahora);
}
