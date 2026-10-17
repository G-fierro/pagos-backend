package com.banduairlines.service.pagos.stubs;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Helper para generar y validar tokens JWT en entornos de desarrollo y pruebas locales.
 * Permite que los endpoints protegidos del módulo de pagos (historial de boletas, admin)
 * puedan validarse sin depender de que el módulo CUENTA esté desplegado.
 */
@Component
public class DevJwtAuthHelper {

    private static final Logger log = LoggerFactory.getLogger(DevJwtAuthHelper.class);

    public static final String DEFAULT_JWT_SECRET = "clave_local_de_desarrollo_no_usar_en_produccion_al_menos_32_caracteres";

    private final String jwtSecret;
    private final long expirationMs;
    private final Key signingKey;

    public DevJwtAuthHelper(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        String effectiveSecret = (jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : DEFAULT_JWT_SECRET;
        this.jwtSecret = effectiveSecret;
        this.expirationMs = expirationMs;
        // Garantizar al menos 256 bits (32 bytes) para HMAC-SHA256
        byte[] keyBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.signingKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generarToken(Long usuarioId, String email, RolUsuario rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("id", usuarioId)
                .claim("email", email)
                .claim("rol", rol.name())
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UsuarioAutenticado validarYExtraerUsuario(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object idObj = claims.get("id");
            Long usuarioId = null;
            if (idObj instanceof Number num) {
                usuarioId = num.longValue();
            } else if (idObj instanceof String str) {
                usuarioId = Long.parseLong(str);
            }

            String email = claims.get("email", String.class);
            if (email == null) {
                email = claims.getSubject();
            }

            String rolStr = claims.get("rol", String.class);
            RolUsuario rol = RolUsuario.PASAJERO;
            if (rolStr != null) {
                try {
                    rol = RolUsuario.valueOf(rolStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Rol desconocido en JWT claim: {}, usando PASAJERO", rolStr);
                }
            }

            return new UsuarioAutenticado(usuarioId, email, rol);
        } catch (Exception e) {
            log.debug("Token JWT inválido o expirado: {}", e.getMessage());
            return null;
        }
    }
}
