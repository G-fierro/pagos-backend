package com.banduairlines.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.Reserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.TramoReservaRepository;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaConsultaService;
import com.banduairlines.service.reserva.ResumenReserva;

@Service
public class ReservaConsultaServiceImpl implements ReservaConsultaService {

    private final ReservaAcceso reservaAcceso;
    private final TramoReservaRepository tramoReservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final BoletoRepository boletoRepository;
    private final ResumenReserva resumenReserva;

    public ReservaConsultaServiceImpl(ReservaAcceso reservaAcceso, TramoReservaRepository tramoReservaRepository,
                                      PasajeroRepository pasajeroRepository, BoletoRepository boletoRepository,
                                      ResumenReserva resumenReserva) {
        this.reservaAcceso = reservaAcceso;
        this.tramoReservaRepository = tramoReservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.boletoRepository = boletoRepository;
        this.resumenReserva = resumenReserva;
    }

    /** Responde en cualquier estado (también CANCELADA o CONFIRMADA): es una consulta. */
    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long reservaId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        return resumenReserva.armar(
                reserva,
                tramoReservaRepository.findByReserva_IdOrderByOrdenAsc(reserva.getId()),
                pasajeroRepository.findByReserva_IdOrderByIdAsc(reserva.getId()),
                boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId()));
    }
}
