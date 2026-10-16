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
        validarInformacionContextual(request);

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

        String observacionContextual = construirObservacionContextual(request);

        SeguimientoVuelo nuevoSeguimiento = SeguimientoVuelo.builder()
                .vuelo(vuelo)
                .estado(request.getNuevoEstado())
                .retrasoMinutos(request.getRetrasoMinutos() != null ? request.getRetrasoMinutos() : 0)
                .observacion(observacionContextual)
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

    private void validarInformacionContextual(ActualizarEstadoRequest request) {
        EstadoVuelo estado = request.getNuevoEstado();

        if (estado == EstadoVuelo.RETRASADO) {
            boolean faltaMotivo = (request.getMotivo() == null || request.getMotivo().trim().isEmpty())
                    && (request.getDescripcion() == null || request.getDescripcion().trim().isEmpty());
            if (faltaMotivo) {
                throw new ReglaNegocioException("Debe adjuntar el motivo o justificación del retraso")
                        .conCodigo("MOTIVO_REQUERIDO");
            }
            if (request.getRetrasoMinutos() == null || request.getRetrasoMinutos() <= 0) {
                throw new ReglaNegocioException("Para registrar un retraso debe indicar los minutos estimados mayores a cero")
                        .conCodigo("RETRASO_INVALIDO");
            }
        }

        if (estado == EstadoVuelo.CANCELADO) {
            boolean faltaMotivo = (request.getMotivo() == null || request.getMotivo().trim().isEmpty())
                    && (request.getDescripcion() == null || request.getDescripcion().trim().isEmpty());
            if (faltaMotivo) {
                throw new ReglaNegocioException("Debe adjuntar el motivo o justificación de la cancelación del vuelo")
                        .conCodigo("MOTIVO_REQUERIDO");
            }
        }
    }

    private String construirObservacionContextual(ActualizarEstadoRequest request) {
        String motivo = request.getMotivo() != null ? request.getMotivo().trim() : "";
        String desc = request.getDescripcion() != null ? request.getDescripcion().trim() : "";

        if (!motivo.isEmpty() && !desc.isEmpty()) {
            String combinada = "[" + motivo + "] " + desc;
            return combinada.length() > 255 ? combinada.substring(0, 255) : combinada;
        } else if (!motivo.isEmpty()) {
            return motivo.length() > 255 ? motivo.substring(0, 255) : motivo;
        } else if (!desc.isEmpty()) {
            return desc.length() > 255 ? desc.substring(0, 255) : desc;
        }
        return null;
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