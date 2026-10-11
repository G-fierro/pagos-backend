package com.banduairlines.security;

import java.util.Optional;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public Optional<UsuarioAutenticado> obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    public UsuarioAutenticado exigir() {
        return obtener().orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException("No hay usuario autenticado en la sesión actual"));
    }

    public Long exigirId() {
        return exigir().id();
    }
}
