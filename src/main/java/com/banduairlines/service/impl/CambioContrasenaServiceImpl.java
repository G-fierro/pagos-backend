package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.SolicitudCambioContrasena;
import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.cuenta.CambioContrasenaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CambioContrasenaServiceImpl implements CambioContrasenaService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final PasswordEncoder passwordEncoder;

    public CambioContrasenaServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioActual usuarioActual,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void cambiar(SolicitudCambioContrasena solicitud) {
        Usuario usuario = obtenerUsuarioActual();

        if (!passwordEncoder.matches(
                solicitud.contrasenaActual(),
                usuario.getPasswordHash()
        )) {
            throw new ReglaNegocioException("La contraseña actual es incorrecta")
                    .conCodigo(CodigosError.CONTRASENA_ACTUAL_INCORRECTA);
        }

        if (!solicitud.nuevaContrasena().equals(solicitud.confirmacionContrasena())) {
            throw new ValidacionException("La nueva contraseña y su confirmacion no coinciden");
        }

        usuario.setPasswordHash(passwordEncoder.encode(solicitud.nuevaContrasena()));
        usuarioRepository.save(usuario);
    }

    private Usuario obtenerUsuarioActual() {
        Long idUsuario = usuarioActual.exigirId();
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "La cuenta autenticada ya no esta disponible"
                ));

        if (usuario.getRol() != RolUsuario.PASAJERO
                && usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("El rol no puede modificar la contraseña");
        }
        return usuario;
    }
}
