package com.banduairlines.service.impl;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.SeguimientoVuelo;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.Vuelo;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.repository.VueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.tracking.TrackingAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingAdminServiceImpl implements TrackingAdminService {

    private final SeguimientoVueloRepository seguimientoRepository;
    private final VueloRepository vueloRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;

    public TrackingAdminServiceImpl(SeguimientoVueloRepository seguimientoRepository,
                                   VueloRepository vueloRepository,
                                   UsuarioRepository usuarioRepository,
                                   UsuarioActual usuarioActual) {
        this.seguimientoRepository = seguimientoRepository;
        this.vueloRepository = vueloRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional
    public SeguimientoVueloResponse actualizarEstadoVuelo(Long vueloId, ActualizarEstadoRequest request) {
        if ((request.getNuevoEstado() == EstadoVuelo.RETRASADO || request.getNuevoEstado() == EstadoVuelo.CANCELADO)
                && (request.getMotivo() == null || request.getMotivo().trim().isEmpty())) {
            throw new ReglaNegocioException("Debe proporcionar un motivo para estados RETRASADO o CANCELADO")
                    .conCodigo("MOTIVO_REQUERIDO");
        }

        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el vuelo con ID: " + vueloId));

        EstadoVuelo estadoAnterior = seguimientoRepository
                .findFirstByVueloIdOrderByRegistradoEnDesc(vueloId)
                .map(SeguimientoVuelo::getEstado)
                .orElse(EstadoVuelo.PROGRAMADO);

        validarTransicion(estadoAnterior, request.getNuevoEstado());

        Long adminId = usuarioActual.exigirId();
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado con ID: " + adminId));

        SeguimientoVuelo nuevoSeguimiento = SeguimientoVuelo.builder()
                .vuelo(vuelo)
                .estado(request.getNuevoEstado())
                .retrasoMinutos(request.getRetrasoMinutos() != null ? request.getRetrasoMinutos() : 0)
                .observacion(request.getMotivo())
                .registradoPor(admin)
                .build();

        SeguimientoVuelo guardado = seguimientoRepository.save(nuevoSeguimiento);

        return new SeguimientoVueloResponse(
                guardado.getId(),
                vuelo.getId(),
                vuelo.getCodigoVuelo(),
                guardado.getEstado(),
                guardado.getRetrasoMinutos(),
                guardado.getObservacion(),
                admin.getId(),
                guardado.getRegistradoEn()
        );
    }

    private void validarTransicion(EstadoVuelo actual, EstadoVuelo nuevo) {
        if (actual == EstadoVuelo.ATERRIZADO || actual == EstadoVuelo.CANCELADO) {
            throw new ReglaNegocioException("No se puede cambiar el estado de un vuelo que ya está " + actual)
                    .conCodigo("TRANSICION_ESTADO_INVALIDA");
        }
        if (actual == nuevo) {
            throw new ReglaNegocioException("El vuelo ya se encuentra en estado " + nuevo)
                    .conCodigo("ESTADO_REDUNDANTE");
        }
    }
}