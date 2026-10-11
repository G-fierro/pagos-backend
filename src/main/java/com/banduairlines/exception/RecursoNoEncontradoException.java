package com.banduairlines.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    private String codigo = CodigosError.RECURSO_NO_ENCONTRADO;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado: " + id);
    }

    public String getCodigo() {
        return codigo;
    }

    public RecursoNoEncontradoException conCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }
}
