package com.banduairlines.service.pagos.stubs;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.security.UsuarioAutenticado;

class DevJwtAuthHelperTest {

    private DevJwtAuthHelper helper;

    @BeforeEach
    void setUp() {
        helper = new DevJwtAuthHelper("clave_secreta_super_segura_de_prueba_para_tests_32_bytes", 3600000L);
    }

    @Test
    void generarYValidarTokenPasajero_extraeClaimsCorrectamente() {
        String token = helper.generarToken(15L, "pasajero@test.com", RolUsuario.PASAJERO);
        assertNotNull(token);
        assertFalse(token.isBlank());

        UsuarioAutenticado usuario = helper.validarYExtraerUsuario(token);
        assertNotNull(usuario);
        assertEquals(15L, usuario.id());
        assertEquals("pasajero@test.com", usuario.email());
        assertEquals(RolUsuario.PASAJERO, usuario.rol());
    }

    @Test
    void generarYValidarTokenAdministrador_extraeClaimsCorrectamente() {
        String token = helper.generarToken(99L, "admin@test.com", RolUsuario.ADMINISTRADOR);
        assertNotNull(token);

        UsuarioAutenticado usuario = helper.validarYExtraerUsuario(token);
        assertNotNull(usuario);
        assertEquals(99L, usuario.id());
        assertEquals("admin@test.com", usuario.email());
        assertEquals(RolUsuario.ADMINISTRADOR, usuario.rol());
    }

    @Test
    void tokenInvalido_retornaNull() {
        UsuarioAutenticado usuario = helper.validarYExtraerUsuario("token.totalmente.invalido");
        assertNull(usuario);
    }
}
