package com.banduairlines.service.impl;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;
import com.banduairlines.model.Reserva;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaContactoService;

@Service
public class ReservaContactoServiceImpl implements ReservaContactoService {

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;

    public ReservaContactoServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
    }

    /**
     * El correo, junto con el código de reserva, es lo que permite al invitado recuperar su compra,
     * por eso se guarda normalizado (sin espacios y en minúsculas). Es obligatorio para iniciar el pago.
     */
    @Override
    @Transactional
    public ContactoResponse registrar(Long reservaId, String compraToken, ContactoRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        reserva.setNombreContacto(request.nombreCompleto().trim());
        reserva.setEmailContacto(request.correo().trim().toLowerCase(Locale.ROOT));
        reserva.setTelefonoContacto(request.telefono().trim());
        reservaRepository.save(reserva);

        return new ContactoResponse(reserva.getNombreContacto(), reserva.getEmailContacto(), reserva.getTelefonoContacto());
    }
}
