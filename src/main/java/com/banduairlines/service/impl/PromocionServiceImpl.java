package com.banduairlines.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Promocion;
import com.banduairlines.repository.PromocionRepository;
import com.banduairlines.service.admin.PromocionService;

@Service
@Transactional
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionServiceImpl(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionRespuesta> listar(Boolean activa) {
        return promocionRepository.listarAdministracion(activa)
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionRespuesta obtener(Long id) {
        return mapearADto(exigirPromocion(id));
    }

    @Override
    public PromocionRespuesta crear(PromocionSolicitud solicitud) {
        if (promocionRepository.existsByCodigo(solicitud.codigo())) {
            throw new ConflictoException("Ya existe un código promocional con el código: " + solicitud.codigo());
        }

        validarVigencia(solicitud.vigenteDesde(), solicitud.vigenteHasta());

        Promocion promocion = Promocion.builder()
                .codigo(solicitud.codigo())
                .tipoDescuento(solicitud.tipoDescuento())
                .valor(solicitud.valor())
                .vigenteDesde(solicitud.vigenteDesde())
                .vigenteHasta(solicitud.vigenteHasta())
                .usosMaximos(solicitud.usosMaximos())
                .usosActuales(0)
                .activa(true)
                .build();

        return mapearADto(promocionRepository.save(promocion));
    }

    @Override
    public PromocionRespuesta actualizar(Long id, PromocionSolicitud solicitud) {
        Promocion promocion = exigirPromocion(id);

        if (promocionRepository.existsByCodigoAndIdNot(solicitud.codigo(), id)) {
            throw new ConflictoException("Ya existe un código promocional con el código: " + solicitud.codigo());
        }

        validarVigencia(solicitud.vigenteDesde(), solicitud.vigenteHasta());

        promocion.setCodigo(solicitud.codigo());
        promocion.setTipoDescuento(solicitud.tipoDescuento());
        promocion.setValor(solicitud.valor());
        promocion.setVigenteDesde(solicitud.vigenteDesde());
        promocion.setVigenteHasta(solicitud.vigenteHasta());
        promocion.setUsosMaximos(solicitud.usosMaximos());

        return mapearADto(promocion);
    }

    @Override
    public PromocionRespuesta cambiarEstado(Long id, boolean activa) {
        Promocion promocion = exigirPromocion(id);
        promocion.setActiva(activa);
        return mapearADto(promocion);
    }

    private Promocion exigirPromocion(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Promoción", id));
    }

    private void validarVigencia(LocalDateTime desde, LocalDateTime hasta) {
        if (!desde.isBefore(hasta)) {
            throw new ReglaNegocioException(
                    "La fecha de inicio debe ser anterior a la fecha de expiración")
                    .conCodigo("VIGENCIA_INVALIDA");
        }
    }

    private PromocionRespuesta mapearADto(Promocion promocion) {
        return new PromocionRespuesta(
                promocion.getId(),
                promocion.getCodigo(),
                promocion.getTipoDescuento(),
                promocion.getValor(),
                promocion.getVigenteDesde(),
                promocion.getVigenteHasta(),
                promocion.getUsosMaximos(),
                promocion.getUsosActuales(),
                promocion.getActiva()
        );
    }
}
