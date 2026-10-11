package com.banduairlines.exception;

public class ServicioExternoException extends RuntimeException {

    private String codigo = CodigosError.SERVICIO_EXTERNO;

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public String getCodigo() {
        return codigo;
    }

    public ServicioExternoException conCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }
}
