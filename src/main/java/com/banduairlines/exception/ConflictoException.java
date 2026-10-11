package com.banduairlines.exception;

public class ConflictoException extends RuntimeException {

    private String codigo = CodigosError.CONFLICTO;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public String getCodigo() {
        return codigo;
    }

    public ConflictoException conCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }
}
