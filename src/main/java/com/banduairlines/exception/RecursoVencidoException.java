package com.banduairlines.exception;

public class RecursoVencidoException extends RuntimeException {

    private final String codigo;

    public RecursoVencidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
