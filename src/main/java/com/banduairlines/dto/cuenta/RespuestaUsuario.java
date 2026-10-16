package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.RolUsuario;

public record RespuestaUsuario(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo,
        RolUsuario rol,
        String nivel
) {
}
