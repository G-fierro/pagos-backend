package com.banduairlines.devdata;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class MockJwtAuthFilterTest {

    private static final String SECRET = "clave_local_de_desarrollo_no_usar_en_produccion";

    private final MockJwtAuthFilter filter = new MockJwtAuthFilter(SECRET);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private String token(String secret, String sub, String email, String rol, long expiraEnMs) {
        return Jwts.builder()
                .setSubject(sub)
                .claim("email", email)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiraEnMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Ejecuta el filtro y devuelve la autenticación que dejó el filtro DENTRO de la petición. */
    private Authentication autenticacionDurante(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        Authentication[] capturada = new Authentication[1];
        filter.doFilter(request, new MockHttpServletResponse(),
                new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
                    @Override
                    protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                        capturada[0] = SecurityContextHolder.getContext().getAuthentication();
                    }
                }));
        return capturada[0];
    }

    @Test
    void tokenValido_autenticaConElUsuarioDelToken() throws Exception {
        Authentication auth = autenticacionDurante("Bearer " + token(SECRET, "1", "javiera@test.com", "PASAJERO", 60_000));

        assertNotNull(auth);
        UsuarioAutenticado usuario = (UsuarioAutenticado) auth.getPrincipal();
        assertEquals(1L, usuario.id());
        assertEquals("javiera@test.com", usuario.email());
        assertEquals(RolUsuario.PASAJERO, usuario.rol());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PASAJERO")));
    }

    @Test
    void tokenDeAdministrador_otorgaElRolAdministrador() throws Exception {
        Authentication auth = autenticacionDurante("Bearer " + token(SECRET, "2", "admin@test.com", "ADMINISTRADOR", 60_000));

        assertEquals(RolUsuario.ADMINISTRADOR, ((UsuarioAutenticado) auth.getPrincipal()).rol());
    }

    @Test
    void sinHeader_noAutentica() throws Exception {
        Authentication auth = autenticacionDurante(null);

        assertTrue(auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado));
    }

    @Test
    void tokenFirmadoConOtroSecreto_noAutentica() throws Exception {
        Authentication auth = autenticacionDurante("Bearer "
                + token("otro_secreto_distinto_de_al_menos_32_caracteres", "1", "a@test.com", "PASAJERO", 60_000));

        assertTrue(auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado));
    }

    @Test
    void tokenVencido_noAutentica() throws Exception {
        Authentication auth = autenticacionDurante("Bearer " + token(SECRET, "1", "a@test.com", "PASAJERO", -60_000));

        assertTrue(auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado));
    }

    @Test
    void tokenConRolDesconocidoOMalFormado_noAutentica() throws Exception {
        assertTrue(noAutentica(autenticacionDurante("Bearer " + token(SECRET, "1", "a@test.com", "SUPERUSUARIO", 60_000))));
        assertTrue(noAutentica(autenticacionDurante("Bearer esto.no.es.un.jwt")));
        assertTrue(noAutentica(autenticacionDurante("Basic abc")));
    }

    private boolean noAutentica(Authentication auth) {
        return auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado);
    }
}
