package com.banduairlines.dto.tracking;

import com.banduairlines.model.enums.EstadoVuelo;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoVuelo nuevoEstado;

    private String motivo;

    private Integer retrasoMinutos;

    public ActualizarEstadoRequest() {
    }

    public ActualizarEstadoRequest(EstadoVuelo nuevoEstado, String motivo, Integer retrasoMinutos) {
        this.nuevoEstado = nuevoEstado;
        this.motivo = motivo;
        this.retrasoMinutos = retrasoMinutos;
    }
}