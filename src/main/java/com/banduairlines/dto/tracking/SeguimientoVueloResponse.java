package com.banduairlines.dto.tracking;

import com.banduairlines.model.enums.EstadoVuelo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SeguimientoVueloResponse {

    private Long idSeguimiento;
    private Long vueloId;
    private String codigoVuelo;
    private EstadoVuelo estadoActual;
    private Integer retrasoMinutos;
    private String observacion;
    private Long registradoPorId;
    private LocalDateTime registradoEn;

    public SeguimientoVueloResponse() {
    }

    public SeguimientoVueloResponse(Long idSeguimiento, Long vueloId, String codigoVuelo,EstadoVuelo estadoActual, Integer retrasoMinutos,String observacion, Long registradoPorId,LocalDateTime registradoEn) {
        this.idSeguimiento = idSeguimiento;
        this.vueloId = vueloId;
        this.codigoVuelo = codigoVuelo;
        this.estadoActual = estadoActual;
        this.retrasoMinutos = retrasoMinutos;
        this.observacion = observacion;
        this.registradoPorId = registradoPorId;
        this.registradoEn = registradoEn;
    }
}