package com.banduairlines.security;

import com.banduairlines.model.enums.RolUsuario;

public record UsuarioAutenticado(Long id, String email, RolUsuario rol) {
}
