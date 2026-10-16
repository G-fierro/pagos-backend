package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.ConfirmacionCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaSolicitudCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.RecursoVencidoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.SolicitudCambioCorreoRepository;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.CambioCorreoService;
import com.banduairlines.service.cuenta.ServicioCorreo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CambioCorreoServiceImpl implements CambioCorreoService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudCambioCorreoRepository solicitudRepository;
    private final UsuarioActual usuarioActual;
    private final PasswordEncoder passwordEncoder;
    private final Optional<ServicioCorreo> servicioCorreo;
    private final Clock clock;
    private final long minutosExpiracion;
    private final SecureRandom secureRandom = new SecureRandom();

    public CambioCorreoServiceImpl(
            UsuarioRepository usuarioRepository,
            SolicitudCambioCorreoRepository solicitudRepository,
            UsuarioActual usuarioActual,
            PasswordEncoder passwordEncoder,
            Optional<ServicioCorreo> servicioCorreo,
            Clock clock,
            @Value("${app.cuenta.cambio-correo.expiracion-minutos:15}")
            long minutosExpiracion
    ) {
        if (minutosExpiracion <= 0) {
            throw new IllegalArgumentException("La expiracion del codigo debe ser positiva");
        }
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioActual = usuarioActual;
        this.passwordEncoder = passwordEncoder;
        this.servicioCorreo = servicioCorreo;
        this.clock = clock;
        this.minutosExpiracion = minutosExpiracion;
    }

    @Override
    @Transactional
    public RespuestaSolicitudCambioCorreo solicitar(
            com.banduairlines.dto.cuenta.SolicitudCambioCorreo solicitud
    ) {
        Usuario usuario = obtenerUsuarioActual();
        String nuevoCorreo = normalizarCorreo(solicitud.nuevoCorreo());

        if (nuevoCorreo.equals(usuario.getEmail())) {
            throw new ValidacionException("El nuevo correo debe ser diferente del correo actual");
        }
        validarCorreoDisponible(nuevoCorreo, usuario.getId());

        Instant ahora = clock.instant();
        invalidarSolicitudesAnteriores(usuario.getId(), ahora);

        String codigo = generarCodigo();
        Instant expiracion = ahora.plus(minutosExpiracion, ChronoUnit.MINUTES);

        com.banduairlines.model.SolicitudCambioCorreo entidad =
                com.banduairlines.model.SolicitudCambioCorreo.builder()
                        .usuario(usuario)
                        .nuevoCorreo(nuevoCorreo)
                        .codigoHash(passwordEncoder.encode(codigo))
                        .fechaCreacion(ahora)
                        .fechaExpiracion(expiracion)
                        .build();

        com.banduairlines.model.SolicitudCambioCorreo guardada =
                solicitudRepository.saveAndFlush(entidad);

        enviarCodigo(nuevoCorreo, codigo, expiracion);

        return new RespuestaSolicitudCambioCorreo(
                guardada.getIdSolicitud(),
                enmascararCorreo(nuevoCorreo),
                expiracion
        );
    }

    @Override
    @Transactional
    public RespuestaUsuario confirmar(
            Long idSolicitud,
            ConfirmacionCambioCorreo confirmacion
    ) {
        Usuario usuario = obtenerUsuarioActual();
        com.banduairlines.model.SolicitudCambioCorreo solicitud =
                buscarSolicitud(idSolicitud, usuario.getId());
        Instant ahora = clock.instant();

        if (!solicitud.puedeUtilizarse()) {
            throw codigoIncorrecto("La solicitud ya fue finalizada");
        }
        if (solicitud.estaVencida(ahora)) {
            throw new RecursoVencidoException(
                    CodigosError.CODIGO_VENCIDO,
                    "El codigo de verificacion ha vencido"
            );
        }
        if (!passwordEncoder.matches(confirmacion.codigo(), solicitud.getCodigoHash())) {
            throw codigoIncorrecto("El codigo de verificacion es incorrecto");
        }

        validarCorreoDisponible(solicitud.getNuevoCorreo(), usuario.getId());

        usuario.setEmail(solicitud.getNuevoCorreo());
        solicitud.setFechaConfirmacion(ahora);
        solicitud.setCodigoHash(null);

        try {
            usuarioRepository.saveAndFlush(usuario);
            solicitudRepository.save(solicitud);
        } catch (DataIntegrityViolationException ex) {
            throw correoYaRegistrado();
        }

        return MapeadorUsuario.aRespuesta(usuario);
    }

    @Override
    @Transactional
    public void cancelar(Long idSolicitud) {
        Usuario usuario = obtenerUsuarioActual();
        com.banduairlines.model.SolicitudCambioCorreo solicitud =
                buscarSolicitud(idSolicitud, usuario.getId());

        if (solicitud.fueCancelada() || solicitud.fueConfirmada()) {
            return;
        }

        solicitud.setFechaCancelacion(clock.instant());
        solicitud.setCodigoHash(null);
        solicitudRepository.save(solicitud);
    }

    private Usuario obtenerUsuarioActual() {
        Long idUsuario = usuarioActual.exigirId();
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "La cuenta autenticada ya no esta disponible"
                ));

        if (usuario.getRol() != RolUsuario.PASAJERO
                && usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("El rol no puede cambiar el correo");
        }
        return usuario;
    }

    private com.banduairlines.model.SolicitudCambioCorreo buscarSolicitud(
            Long idSolicitud,
            Long idUsuario
    ) {
        return solicitudRepository
                .findByIdSolicitudAndUsuario_Id(idSolicitud, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La solicitud de cambio de correo no existe"
                ).conCodigo(CodigosError.SOLICITUD_NO_ENCONTRADA));
    }

    private void invalidarSolicitudesAnteriores(Long idUsuario, Instant ahora) {
        List<com.banduairlines.model.SolicitudCambioCorreo> anteriores =
                solicitudRepository
                .findByUsuario_IdAndFechaConfirmacionIsNullAndFechaCancelacionIsNull(
                                idUsuario
                        );

        anteriores.forEach(anterior -> {
            anterior.setFechaCancelacion(ahora);
            anterior.setCodigoHash(null);
        });
        solicitudRepository.saveAll(anteriores);
    }

    private void validarCorreoDisponible(String correo, Long idUsuarioActual) {
        usuarioRepository.findByEmail(correo).ifPresent(usuario -> {
            if (!usuario.getId().equals(idUsuarioActual)) {
                throw correoYaRegistrado();
            }
        });
    }

    private void enviarCodigo(String correo, String codigo, Instant expiracion) {
        ServicioCorreo correoCompartido = servicioCorreo.orElseThrow(() ->
                new ServicioExternoException(
                        "El servicio compartido de correo aun no esta disponible"
                ));
        try {
            correoCompartido.enviarCodigoCambioCorreo(correo, codigo, expiracion);
        } catch (ServicioExternoException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ServicioExternoException(
                    "No fue posible enviar el codigo de cambio de correo",
                    ex
            );
        }
    }

    private String generarCodigo() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String enmascararCorreo(String correo) {
        int separador = correo.indexOf('@');
        return correo.substring(0, 1) + "*****" + correo.substring(separador);
    }

    private ConflictoException correoYaRegistrado() {
        return new ConflictoException("El correo ya se encuentra registrado")
                .conCodigo(CodigosError.CORREO_YA_REGISTRADO);
    }

    private ReglaNegocioException codigoIncorrecto(String mensaje) {
        return new ReglaNegocioException(mensaje)
                .conCodigo(CodigosError.CODIGO_INCORRECTO);
    }
}
