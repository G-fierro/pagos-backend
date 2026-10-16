package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudRegistro;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.service.cuenta.RegistroPasajeroService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistroPasajeroServiceImpl implements RegistroPasajeroService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroPasajeroServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RespuestaUsuario registrar(SolicitudRegistro solicitud) {
        validarCamposReservados(solicitud);
        validarConfirmacion(solicitud.contrasena(), solicitud.confirmacionContrasena());

        String correo = normalizarCorreo(solicitud.correo());
        validarCorreoDisponible(correo);

        Usuario usuario = Usuario.builder()
                .nombre(solicitud.nombres().trim())
                .apellido(solicitud.apellidos().trim())
                .email(correo)
                .passwordHash(passwordEncoder.encode(solicitud.contrasena()))
                .rol(RolUsuario.PASAJERO)
                .activo(true)
                .build();

        CuentaFidelizacion cuenta = CuentaFidelizacion.builder()
                .usuario(usuario)
                .saldoMillas(0)
                .nivel(Nivel.BRONCE)
                .build();

        usuario.setCuentaFidelizacion(cuenta);

        try {
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);
            return MapeadorUsuario.aRespuesta(guardado);
        } catch (DataIntegrityViolationException ex) {
            throw correoYaRegistrado();
        }
    }

    private void validarCamposReservados(SolicitudRegistro solicitud) {
        if (solicitud.rol() != null || solicitud.nivel() != null) {
            throw new ValidacionException("El rol y el nivel se asignan automaticamente");
        }
    }

    private void validarConfirmacion(String contrasena, String confirmacion) {
        if (!contrasena.equals(confirmacion)) {
            throw new ValidacionException("La contraseña y su confirmacion no coinciden");
        }
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void validarCorreoDisponible(String correo) {
        if (usuarioRepository.existsByEmail(correo)) {
            throw correoYaRegistrado();
        }
    }

    private ConflictoException correoYaRegistrado() {
        return new ConflictoException("El correo ya se encuentra registrado")
                .conCodigo(CodigosError.CORREO_YA_REGISTRADO);
    }
}
