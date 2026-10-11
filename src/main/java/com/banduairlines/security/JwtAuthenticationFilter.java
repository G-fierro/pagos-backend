package com.banduairlines.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        /* 
           ======================= INICIO ZONA MÓDULO CUENTA =======================
           TODO(CUENTA): implementar la validación del JWT aquí. Pasos esperados:
            1. Leer el header "Authorization: Bearer <token>". Si no existe, continuar sin autenticar.
            2. Validar firma y expiración con la clave app.jwt.secret.
            3. Si es válido, crear el Authentication y dejarlo en el contexto:
                 var principal = new UsuarioAutenticado(id, email, rol);
                 var auth = new UsernamePasswordAuthenticationToken(
                         principal, null,
                         List.of(new SimpleGrantedAuthority("ROLE_" + rol)));   // ver sección 7 del README
                 SecurityContextHolder.getContext().setAuthentication(auth);
            4. Si el token es inválido o expiró: NO lanzar excepción; SecurityContextHolder.clearContext()
               y continuar. La cadena responderá 401 por sí sola (RestAuthenticationEntryPoint).
           ======================== FIN ZONA MÓDULO CUENTA =========================
        */
        chain.doFilter(request, response);
    }
}
