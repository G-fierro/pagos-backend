package com.banduairlines.service.reserva;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Credencial de compra del invitado (header X-Compra-Token).
 * Es un HMAC del id de la reserva, así que no necesita columna en la tabla reservas:
 * POST /reservas lo entrega con generar(id) y el resto de los endpoints lo verifica con esValido.
 */
@Component
public class CompraTokenService {

    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec clave;

    public CompraTokenService(@Value("${app.reserva.compra-token-secret:${app.jwt.secret}}") String secreto) {
        this.clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO);
    }

    public String generar(Long reservaId) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            byte[] firma = mac.doFinal(("reserva:" + reservaId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar el token de compra", e);
        }
    }

    public boolean esValido(Long reservaId, String token) {
        if (reservaId == null || token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                generar(reservaId).getBytes(StandardCharsets.UTF_8),
                token.trim().getBytes(StandardCharsets.UTF_8));
    }
}
