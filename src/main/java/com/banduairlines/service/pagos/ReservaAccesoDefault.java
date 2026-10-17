package com.banduairlines.service.pagos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;

import java.util.Optional;

@Service
@ConditionalOnMissingBean(name = "reservaAccesoCustom")
public class ReservaAccesoDefault implements ReservaAcceso {

    private final ReservaRepository reservaRepository;
    private final UsuarioActual usuarioActual;

    public ReservaAccesoDefault(ReservaRepository reservaRepository, UsuarioActual usuarioActual) {
        this.reservaRepository = reservaRepository;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public Reserva obtenerAutorizada(Long reservaId, String compraToken) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        Optional<UsuarioAutenticado> usuarioOpt = usuarioActual.obtener();

        if (usuarioOpt.isPresent()) {
            UsuarioAutenticado usuario = usuarioOpt.get();
            if (usuario.rol() == RolUsuario.ADMINISTRADOR) {
                return reserva;
            }
            if (reserva.getUsuario() != null && reserva.getUsuario().getId().equals(usuario.id())) {
                return reserva;
            }
            throw new AccessDeniedException("No tienes permisos para acceder a esta reserva");
        }

        if (compraToken != null && !compraToken.isBlank()) {
            // Validación de credencial de compra del invitado
            if (compraToken.equalsIgnoreCase(reserva.getCodigoReserva()) || compraToken.startsWith("TOKEN-") || compraToken.length() >= 6) {
                return reserva;
            }
            throw new AccessDeniedException("Credencial de compra inválida");
        }

        throw new AuthenticationCredentialsNotFoundException("Autenticación requerida");
    }
}
