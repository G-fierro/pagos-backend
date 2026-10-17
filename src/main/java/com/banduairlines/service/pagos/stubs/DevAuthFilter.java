package com.banduairlines.service.pagos.stubs;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticación para desarrollo y pruebas del módulo de pagos.
 * Intercepta cabeceras Bearer JWT o tokens de prueba (dev-admin, dev-pasajero)
 * e inyecta el UsuarioAutenticado en el SecurityContextHolder de Spring Security,
 * permitiendo ejecutar las pruebas de integración y contratos sin el módulo CUENTA.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class DevAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DevAuthFilter.class);

    public static final String DEFAULT_SERVICE_KEY = "clave_compartida_reservas";

    private final DevJwtAuthHelper devJwtAuthHelper;
    private final String serviceKey;

    public DevAuthFilter(
            DevJwtAuthHelper devJwtAuthHelper,
            @Value("${bandu.internas.service-key:clave_compartida_reservas}") String serviceKey
    ) {
        this.devJwtAuthHelper = devJwtAuthHelper;
        this.serviceKey = (serviceKey != null && !serviceKey.isBlank()) ? serviceKey : DEFAULT_SERVICE_KEY;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                UsuarioAutenticado usuario = null;

                // Atajos de conveniencia para pruebas rápidas
                if ("dev-admin".equalsIgnoreCase(token)) {
                    usuario = new UsuarioAutenticado(99L, "admin@banduairlines.com", RolUsuario.ADMINISTRADOR);
                } else if ("dev-pasajero".equalsIgnoreCase(token)) {
                    usuario = new UsuarioAutenticado(1L, "pasajero@banduairlines.com", RolUsuario.PASAJERO);
                } else {
                    usuario = devJwtAuthHelper.validarYExtraerUsuario(token);
                }

                if (usuario != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Sesión autenticada en pagos (dev): id={}, email={}, rol={}",
                            usuario.id(), usuario.email(), usuario.rol());
                }
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String requestUri = request.getRequestURI();
                if (requestUri != null && requestUri.startsWith("/api/v1/pagos/dev")) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            new UsuarioAutenticado(0L, "dev@banduairlines.com", RolUsuario.ADMINISTRADOR),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"), new SimpleGrantedAuthority("ROLE_DEV"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Si la petición viene con X-Service-Key válida para endpoints internos
                    String keyHeader = request.getHeader("X-Service-Key");
                    if (keyHeader != null && keyHeader.equals(serviceKey)) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                new UsuarioAutenticado(0L, "servicio-interno@banduairlines.com", RolUsuario.ADMINISTRADOR),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"), new SimpleGrantedAuthority("ROLE_INTERNO"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error evaluando autenticación dev en pagos: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
