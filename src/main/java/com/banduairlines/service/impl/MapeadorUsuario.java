package com.banduairlines.service.impl;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.model.CuentaFidelizacion;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.Nivel;

final class MapeadorUsuario {

    private MapeadorUsuario() {
    }

    static RespuestaUsuario aRespuesta(Usuario usuario) {
        CuentaFidelizacion cuenta = usuario.getCuentaFidelizacion();
        Nivel nivel = cuenta != null ? cuenta.getNivel() : null;

        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                nivel
        );
    }
}
