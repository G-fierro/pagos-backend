package com.banduairlines.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.banduairlines.model.Ruta;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    @Query("""
            select ruta
            from Ruta ruta
            join fetch ruta.origen
            join fetch ruta.destino
            where (:activa is null or ruta.activa = :activa)
            """)
    Page<Ruta> listarAdministracion(@Param("activa") Boolean activa,
            Pageable paginacion);

    @Query("""
            select count(ruta)
            from Ruta ruta
            where ruta.origen.id = :origenId
              and ruta.destino.id = :destinoId
              and (:excluirId is null or ruta.id <> :excluirId)
            """)
    long contarDuplicadasAdministracion(
            @Param("origenId") Long origenId,
            @Param("destinoId") Long destinoId,
            @Param("excluirId") Long exluirId);

    @Query("""
            select count(vuelo)
            from Vuelo vuelo
            where vuelo.ruta.id = :rutaId
            """)
    long contarVuelosAsociados(@Param("rutaId") Long rutaId);

    @Query("""
            select ruta
            from Ruta ruta
            join fetch ruta.origen
            join fetch ruta.destino
            where (:activa is null or ruta.activa = :activa)
            order by ruta.id asc
            """)
    List<Ruta> listarTodasAdministracion(
            @Param("activa") Boolean activa);

    @Query(value = """
            select ruta
            from Ruta ruta
            join fetch ruta.origen origen
            join fetch ruta.destino destino
            where (:activa is null or ruta.activa = :activa)
              and (
                lower(origen.codigoIata) like :patron escape '!'
                or lower(destino.codigoIata) like :patron escape '!'
                or lower(origen.ciudad) like :patron escape '!'
                or lower(destino.ciudad) like :patron escape '!'
                or lower(
                    concat(concat(origen.codigoIata, '-'), destino.codigoIata)
                ) like :patron escape '!'
              )
            """, countQuery = """
            select count(ruta)
            from Ruta ruta
            join ruta.origen origen
            join ruta.destino destino
            where (:activa is null or ruta.activa = :activa)
              and (
                lower(origen.codigoIata) like :patron escape '!'
                or lower(destino.codigoIata) like :patron escape '!'
                or lower(origen.ciudad) like :patron escape '!'
                or lower(destino.ciudad) like :patron escape '!'
                or lower(
                    concat(concat(origen.codigoIata, '-'), destino.codigoIata)
                ) like :patron escape '!'
              )
            """)
    Page<Ruta> buscarAdministracion(
            @Param("activa") Boolean activa,
            @Param("patron") String patron,
            Pageable paginacion);
}
