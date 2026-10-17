package com.banduairlines.devdata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MOCK TEMPORAL de la validación del JWT, solo para probar localmente los endpoints de pasajero
 * mientras el módulo CUENTA no completa {@code JwtAuthenticationFilter}.
 *
 * <ul>
 *   <li>Está APAGADO por defecto: solo se registra con {@code app.mockjwt.enabled=true}
 *       (variable de entorno {@code APP_MOCKJWT_ENABLED=true}). Nunca debe activarse en un servidor.</li>
 *   <li>Valida un JWT HS256 firmado con {@code app.jwt.secret} y los claims del README (sección 7):
 *       {@code sub} (id del usuario), {@code email} y {@code rol} (PASAJERO | ADMINISTRADOR).</li>
 *   <li>No emite tokens: el cliente (por ejemplo Postman) firma uno con el mismo secreto.</li>
 *   <li>Un token ausente, inválido o vencido no autentica; la cadena responde 401 por sí sola.</li>
 * </ul>
 *
 * Se elimina junto con el resto de {@code devdata} cuando CUENTA entregue el filtro real
 * (ver LEEME.md).
 */
@Component
@ConditionalOnProperty(name = "app.mockjwt.enabled", havingValue = "true")
public class MockJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MockJwtAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final String secret;

    public MockJwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
        log.warn("MOCK DE JWT ACTIVO (app.mockjwt.enabled=true): solo para pruebas locales, "
                + "hasta que CUENTA entregue la validación real.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(header.substring(BEARER.length()).trim())
                        .getBody();

                Long id = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                RolUsuario rol = RolUsuario.valueOf(claims.get("rol", String.class));

                var principal = new UsuarioAutenticado(id, email, rol);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException | NullPointerException e) {
                // Token inválido, vencido o con claims incompletos: no se autentica (la cadena responde 401).
                log.debug("JWT rechazado por el mock: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
