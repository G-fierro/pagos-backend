package com.banduairlines.repository;

import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VueloRepository extends JpaRepository<Vuelo, Long> {

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(o.codigoIata) = UPPER(:origenIata)
              AND UPPER(d.codigoIata) = UPPER(:destinoIata)
              AND r.activa = true
              AND v.estado IN :estadosValidos
              AND v.salidaProgramada >= :inicioRango
              AND v.salidaProgramada < :finRango
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarVuelosDirectos(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("inicioRango") LocalDateTime inicioRango,
            @Param("finRango") LocalDateTime finRango,
            @Param("estadosValidos") Collection<EstadoVuelo> estadosValidos
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(o.codigoIata) = UPPER(:origenIata)
              AND UPPER(d.codigoIata) <> UPPER(:destinoIata)
              AND r.activa = true
              AND v.estado IN :estadosValidos
              AND v.salidaProgramada >= :inicioRango
              AND v.salidaProgramada < :finRango
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarPrimerosTramosParaConexion(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("inicioRango") LocalDateTime inicioRango,
            @Param("finRango") LocalDateTime finRango,
            @Param("estadosValidos") Collection<EstadoVuelo> estadosValidos
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE UPPER(d.codigoIata) = UPPER(:destinoIata)
              AND UPPER(o.codigoIata) <> UPPER(:origenIata)
              AND r.activa = true
              AND v.estado IN :estadosValidos
              AND v.salidaProgramada >= :inicioVentana
              AND v.salidaProgramada <= :finVentana
            ORDER BY v.salidaProgramada ASC
            """)
    List<Vuelo> buscarSegundosTramosParaConexion(
            @Param("origenIata") String origenIata,
            @Param("destinoIata") String destinoIata,
            @Param("inicioVentana") LocalDateTime inicioVentana,
            @Param("finVentana") LocalDateTime finVentana,
            @Param("estadosValidos") Collection<EstadoVuelo> estadosValidos
    );

    @Query("""
            SELECT v FROM Vuelo v
            JOIN FETCH v.ruta r
            JOIN FETCH r.origen o
            JOIN FETCH r.destino d
            JOIN FETCH v.aeronave a
            WHERE v.id = :id
            """)
    Optional<Vuelo> findByIdConDetalles(@Param("id") Long id);
}