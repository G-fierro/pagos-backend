package com.banduairlines.dto.tracking;

import com.banduairlines.model.enums.EstadoVuelo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoVuelo nuevoEstado;

    @Size(max = 100, message = "El motivo no puede exceder los 100 caracteres")
    private String motivo;

    @Size(max = 255, message = "La justificación o descripción no puede exceder los 255 caracteres")
    private String descripcion;

    private Integer retrasoMinutos;

    public ActualizarEstadoRequest() {
    }

    public ActualizarEstadoRequest(EstadoVuelo nuevoEstado, String motivo, String descripcion, Integer retrasoMinutos) {
        this.nuevoEstado = nuevoEstado;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.retrasoMinutos = retrasoMinutos;
    }
}