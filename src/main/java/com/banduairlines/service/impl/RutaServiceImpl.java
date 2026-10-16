package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.RutaRespuesta;
import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Ruta;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.RutaRepository;
import com.banduairlines.service.admin.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RutaServiceImpl implements RutaService {
    private final RutaRepository rutas;
    private final AeropuertoRepository aeropuertos;

    @Override
    public Page<RutaRespuesta> listar(
            Boolean activa, int pagina, int tamaño) {
        validarPaginacion(pagina, tamaño);
        var paginacion = PageRequest.of(pagina, tamaño, Sort.by("id").ascending());
        return rutas.listarAdministracion(activa, paginacion).map(this::convertirRespuesta);
    }

    @Override
    public RutaRespuesta obtener(Long id) {
        return convertirRespuesta(buscarRuta(id));
    }

    @Override
    @Transactional
    public RutaRespuesta crear(RutaSolicitud solicitud) {
        validarSolicitud(solicitud, null);

        Ruta ruta = new Ruta();
        asignarDatos(ruta, solicitud);

        return convertirRespuesta(rutas.saveAndFlush(ruta));
    }

    @Override
    @Transactional
    public RutaRespuesta actualizar(Long id, RutaSolicitud solicitud) {
        Ruta ruta = buscarRuta(id);

        boolean cambiaRecorrido = !ruta.getOrigen().getId().equals(solicitud.origenId())
                || !ruta.getDestino().getId().equals(solicitud.destinoId());

        if (cambiaRecorrido && rutas.contarVuelosAsociados(id) > 0) {
            throw new ConflictoException(
                    "No se puede cambiar el origen o destino "
                            + "de una ruta con vuelos asociados. "
                            + "Crea una nueva ruta para el nuevo recorrido.")
                    .conCodigo("RUTA_CON_VUELOS_ASOCIADOS");
        }

        validarSolicitud(solicitud, id);
        asignarDatos(ruta, solicitud);

        return convertirRespuesta(rutas.saveAndFlush(ruta));
    }

    @Override
    @Transactional
    public RutaRespuesta cambiarEstado(Long id, boolean activa) {
        Ruta ruta = buscarRuta(id);
        ruta.setActiva(activa);
        return convertirRespuesta(rutas.saveAndFlush(ruta));
    }

    private void validarSolicitud(RutaSolicitud solicitud, Long excluirId) {
        if (solicitud.origenId().equals(solicitud.destinoId())) {
            throw new ReglaNegocioException("el aeropuerto de origen y destino deber ser diferentes");
        }
        long duplicadas = rutas.contarDuplicadasAdministracion(solicitud.origenId(), solicitud.destinoId(), excluirId);
        if (duplicadas > 0) {
            throw new ConflictoException("Ya existe una ruta con ese origen y destrino");
        }
    }

    public void asignarDatos(Ruta ruta, RutaSolicitud solicitud) {
        Aeropuerto origen = buscarAeropuerto(solicitud.origenId());
        Aeropuerto destino = buscarAeropuerto(solicitud.destinoId());

        ruta.setOrigen(origen);
        ruta.setDestino(destino);
        ruta.setDistanciaKm(solicitud.distanciaKm());
        ruta.setDuracionMin(solicitud.duracionMin());
        ruta.setActiva(solicitud.activa());
    }

    private Ruta buscarRuta(Long id) {
        return rutas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Ruta", id));
    }

    private Aeropuerto buscarAeropuerto(Long id) {
        return aeropuertos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Aeropuerto", id));
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100
                || (long) pagina * tamano > Integer.MAX_VALUE) {

            throw new ReglaNegocioException(
                    "La página debe comenzar en 0 y el tamaño "
                            + "debe estar entre 1 y 100, sin exceder "
                            + "el desplazamiento permitido");
        }
    }

    private RutaRespuesta convertirRespuesta(Ruta ruta) {
        return new RutaRespuesta(
                ruta.getId(),
                ruta.getOrigen().getId(),
                ruta.getOrigen().getCodigoIata(),
                ruta.getOrigen().getCiudad(),
                ruta.getDestino().getId(),
                ruta.getDestino().getCodigoIata(),
                ruta.getDestino().getCiudad(),
                ruta.getDistanciaKm(),
                ruta.getDuracionMin(),
                ruta.getActiva());
    }

}
