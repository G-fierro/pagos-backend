package com.banduairlines.security;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.banduairlines.model.enums.RolUsuario;

class UsuarioActualTest {

    private UsuarioActual usuarioActual;

    @BeforeEach
    void setUp() {
        usuarioActual = new UsuarioActual();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtener_cuandoNoHayAutenticacion_retornaVacio() {
        SecurityContextHolder.clearContext();

        Optional<UsuarioAutenticado> resultado = usuarioActual.obtener();

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtener_cuandoPrincipalNoEsUsuarioAutenticado_retornaVacio() {
        var auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UsuarioAutenticado> resultado = usuarioActual.obtener();

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtener_cuandoHayUsuarioAutenticado_retornaUsuario() {
        UsuarioAutenticado esperado = new UsuarioAutenticado(42L, "test@banduairlines.com", RolUsuario.PASAJERO);
        var auth = new UsernamePasswordAuthenticationToken(
                esperado, null, List.of(new SimpleGrantedAuthority("ROLE_PASAJERO")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UsuarioAutenticado> resultado = usuarioActual.obtener();

        assertThat(resultado).contains(esperado);
    }

    @Test
    void exigir_cuandoHayUsuario_retornaUsuario() {
        UsuarioAutenticado esperado = new UsuarioAutenticado(1L, "admin@banduairlines.com", RolUsuario.ADMINISTRADOR);
        var auth = new UsernamePasswordAuthenticationToken(
                esperado, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioAutenticado resultado = usuarioActual.exigir();

        assertThat(resultado).isEqualTo(esperado);
        assertThat(usuarioActual.exigirId()).isEqualTo(1L);
    }

    @Test
    void exigir_cuandoNoHayUsuario_lanzaAuthenticationCredentialsNotFoundException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> usuarioActual.exigir())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("No hay usuario autenticado");

        assertThatThrownBy(() -> usuarioActual.exigirId())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
