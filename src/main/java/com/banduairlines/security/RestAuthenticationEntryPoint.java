package com.banduairlines.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.banduairlines.exception.CodigosError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.escribir(response, HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO,
                "Autenticación requerida o token inválido", request.getRequestURI());
    }
}
