package com.banduairlines.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_cuandoVariablesVacias_noHaceNada() {
        AdministradorInicial inicial = new AdministradorInicial(
                usuarioRepository, passwordEncoder, "", ""
        );

        inicial.run(null);

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_cuandoSoloUnaVariableDefinida_noHaceNada() {
        AdministradorInicial inicial = new AdministradorInicial(
                usuarioRepository, passwordEncoder, "admin@banduairlines.com", ""
        );

        inicial.run(null);

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_cuandoPasswordCorta_rechazaYNoGuarda() {
        AdministradorInicial inicial = new AdministradorInicial(
                usuarioRepository, passwordEncoder, "admin@banduairlines.com", "corta123"
        );

        inicial.run(null);

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_cuandoAdminYaExiste_noHaceNada() {
        AdministradorInicial inicial = new AdministradorInicial(
                usuarioRepository, passwordEncoder, "Admin@BanduAirlines.com ", "passwordSeguro123"
        );
        when(usuarioRepository.existsByEmail("admin@banduairlines.com")).thenReturn(true);

        inicial.run(null);

        verify(usuarioRepository).existsByEmail("admin@banduairlines.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_cuandoAdminNoExiste_creaUsuarioAdministrador() {
        AdministradorInicial inicial = new AdministradorInicial(
                usuarioRepository, passwordEncoder, " Admin@BanduAirlines.com ", "passwordSeguro123"
        );
        when(usuarioRepository.existsByEmail("admin@banduairlines.com")).thenReturn(false);
        when(passwordEncoder.encode("passwordSeguro123")).thenReturn("encodedPasswordHash");

        inicial.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo("admin@banduairlines.com");
        assertThat(guardado.getPasswordHash()).isEqualTo("encodedPasswordHash");
        assertThat(guardado.getRol()).isEqualTo(RolUsuario.ADMINISTRADOR);
        assertThat(guardado.getNombre()).isEqualTo("Administrador");
        assertThat(guardado.getApellido()).isEqualTo("Sistema");
        assertThat(guardado.getActivo()).isTrue();
    }
}
