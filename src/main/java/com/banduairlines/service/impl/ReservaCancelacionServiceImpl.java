package com.banduairlines.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ReservaCanceladaResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaCancelacionService;

@Service
@Transactional
public class ReservaCancelacionServiceImpl implements ReservaCancelacionService {

    private static final Logger log = LoggerFactory.getLogger(ReservaCancelacionServiceImpl.class);

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;

    public ReservaCancelacionServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                         BoletoRepository boletoRepository) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
    }

    @Override
    public ReservaCanceladaResponse cancelar(Long reservaId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            throw new ConflictoException("La reserva ya está confirmada; su cancelación se gestiona en Gestión del Viaje")
                    .conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva ya está cancelada").conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        cancelarYLiberar(reserva);
        return new ReservaCanceladaResponse(reserva.getId(), reserva.getCodigoReserva(), reserva.getEstado());
    }

    @Override
    @Scheduled(fixedDelayString = "${app.reserva.expiracion-intervalo-ms:60000}")
    public int cancelarExpiradas() {
        List<Reserva> vencidas = reservaRepository.findByEstadoInAndExpiraEnBefore(ReservaAcceso.ABIERTAS, reservaAcceso.ahora());
        vencidas.forEach(this::cancelarYLiberar);
        if (!vencidas.isEmpty()) {
            log.info("Se cancelaron {} reservas por vencimiento del bloqueo", vencidas.size());
        }
        return vencidas.size();
    }

    private void cancelarYLiberar(Reserva reserva) {
        reserva.setEstado(EstadoReserva.CANCELADA);
        boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId())
                .forEach(boleto -> boleto.setAsientoId(null));
        reservaRepository.save(reserva);
    }
}
