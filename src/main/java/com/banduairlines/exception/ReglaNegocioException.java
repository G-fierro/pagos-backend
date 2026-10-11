package com.banduairlines.exception;

public class ReglaNegocioException extends RuntimeException {

    private String codigo = CodigosError.REGLA_NEGOCIO;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public String getCodigo() {
        return codigo;
    }

    public ReglaNegocioException conCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }
}
