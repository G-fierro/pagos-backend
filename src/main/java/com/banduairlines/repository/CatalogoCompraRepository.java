package com.banduairlines.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.banduairlines.model.Asiento;
import com.banduairlines.model.PackTarifario;
import com.banduairlines.model.TarifaVuelo;
import com.banduairlines.model.TramoReserva;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.ClaseCabina;

import jakarta.persistence.LockModeType;

/**
 * Consultas de solo lectura que RESERVA hace sobre el catálogo de ADMIN y BÚSQUEDA (asientos, packs, tarifas, vuelos).
 * Viven aquí para no modificar los repositorios de esos módulos. No guarda ni modifica nada de ellos.
 */
public interface CatalogoCompraRepository extends Repository<TramoReserva, Long> {

    /**
     * Bloquea la fila del vuelo hasta el fin de la transacción (SELECT ... FOR UPDATE), para que dos compras
     * no tomen a la vez el último cupo o el mismo asiento. No modifica el vuelo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vuelo v where v.id = :id")
    Optional<Vuelo> bloquearVuelo(@Param("id") Long id);

    @Query("select p from PackTarifario p where lower(p.nombre) = lower(:nombre)")
    Optional<PackTarifario> buscarPackPorNombre(@Param("nombre") String nombre);

    /** Única por la restricción (vuelo_id, clase_cabina, pack_id) de tarifas_vuelo. */
    @Query("select t from TarifaVuelo t where t.vuelo.id = :vueloId and t.claseCabina = :cabina and t.pack.id = :packId")
    Optional<TarifaVuelo> buscarTarifa(@Param("vueloId") Long vueloId,
                                       @Param("cabina") ClaseCabina cabina,
                                       @Param("packId") Long packId);

    @Query("select count(a) from Asiento a where a.aeronave.id = :aeronaveId and a.claseCabina = :cabina")
    long contarAsientos(@Param("aeronaveId") Long aeronaveId, @Param("cabina") ClaseCabina cabina);

    @Query("select a from Asiento a where a.aeronave.id = :aeronaveId order by a.fila, a.letra")
    List<Asiento> asientosDeAeronave(@Param("aeronaveId") Long aeronaveId);
}
