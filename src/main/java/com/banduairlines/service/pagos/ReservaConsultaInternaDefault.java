package com.banduairlines.service.pagos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;

import java.time.Instant;
import java.time.ZoneOffset;
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

        List<Pasajero> pasajerosEntities = pasajeroRepository.findByReservaIdOrderByIdAsc(reservaId);
        Pasajero primerAdulto = pasajerosEntities.stream()
                .filter(p -> p.getTipo() == TipoPasajero.ADULTO)
                .findFirst()
                .orElse(null);

        String nombreCompleto = null;
        if (reserva.getUsuario() != null) {
            Usuario u = reserva.getUsuario();
            nombreCompleto = ((u.getNombre() != null ? u.getNombre() : "") + " "
                    + (u.getApellido() != null ? u.getApellido() : "")).trim();
        }
        if ((nombreCompleto == null || nombreCompleto.isEmpty()) && primerAdulto != null) {
            nombreCompleto = (primerAdulto.getNombre() + " " + primerAdulto.getApellido()).trim();
        }
        if (nombreCompleto == null || nombreCompleto.isEmpty()) {
            nombreCompleto = reserva.getEmailContacto();
        }

        ContactoDto contacto = new ContactoDto(
                nombreCompleto,
                reserva.getEmailContacto(),
                reserva.getTelefonoContacto()
        );

        List<PasajeroCompraDto> pasajeros = pasajerosEntities.stream()
                .map(p -> new PasajeroCompraDto(p.getId(), p.getNombre(), p.getApellido()))
                .toList();

        int totalCLP = reserva.getTotal() != null ? reserva.getTotal().intValue() : 0;
        int descuentoCLP = reserva.getDescuento() != null ? reserva.getDescuento().intValue() : 0;
        int subtotalCLP = reserva.getSubtotal() != null ? reserva.getSubtotal().intValue() : totalCLP + descuentoCLP;

        List<LineaCompraDto> lineas = List.of(new LineaCompraDto("TARIFA", "Tarifa de vuelo", subtotalCLP));

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
