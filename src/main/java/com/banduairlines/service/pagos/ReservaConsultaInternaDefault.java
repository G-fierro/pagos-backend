package com.banduairlines.service.pagos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnMissingBean(name = "reservaConsultaInternaCustom")
public class ReservaConsultaInternaDefault implements ReservaConsultaInterna {

    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;

    public ReservaConsultaInternaDefault(ReservaRepository reservaRepository, PasajeroRepository pasajeroRepository) {
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
    }

    @Override
    public ReservaParaPago obtenerParaPago(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        String nombreCompleto = "Cliente";
        String rut = null;
        if (reserva.getUsuario() != null) {
            Usuario u = reserva.getUsuario();
            nombreCompleto = (u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "");
            nombreCompleto = nombreCompleto.trim().isEmpty() ? u.getEmail() : nombreCompleto.trim();
        }

        ContactoDto contacto = new ContactoDto(
                nombreCompleto,
                reserva.getEmailContacto() != null ? reserva.getEmailContacto() : "cliente@banduairlines.com",
                reserva.getTelefonoContacto() != null ? reserva.getTelefonoContacto() : "+56912345678"
        );

        List<Pasajero> pasajerosEntities = pasajeroRepository.findAll().stream()
                .filter(p -> p.getReserva() != null && p.getReserva().getId().equals(reservaId))
                .toList();

        List<PasajeroCompraDto> pasajeros = new ArrayList<>();
        if (!pasajerosEntities.isEmpty()) {
            for (Pasajero p : pasajerosEntities) {
                pasajeros.add(new PasajeroCompraDto(p.getId(), p.getNombre(), p.getApellido()));
            }
        } else {
            pasajeros.add(new PasajeroCompraDto(1L, contacto.nombreCompleto(), ""));
        }

        int totalCLP = reserva.getTotal() != null ? reserva.getTotal().intValue() : 0;
        int descuentoCLP = reserva.getDescuento() != null ? reserva.getDescuento().intValue() : 0;

        List<LineaCompraDto> lineas = List.of(
                new LineaCompraDto("TARIFA", "Tarifa de vuelo", totalCLP + descuentoCLP)
        );

        TotalReservaDto total = new TotalReservaDto(lineas, descuentoCLP, totalCLP);

        Instant expiraEn = reserva.getExpiraEn() != null
                ? reserva.getExpiraEn().toInstant(ZoneOffset.UTC)
                : Instant.now().plusSeconds(1800);

        return new ReservaParaPago(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEstado(),
                expiraEn,
                contacto,
                pasajeros,
                total
        );
    }
}
