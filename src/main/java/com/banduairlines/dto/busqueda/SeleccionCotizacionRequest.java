package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeleccionCotizacionRequest {

    // --- Identificadores ---

    @NotNull
    private Long vueloId;

    @NotNull
    private Long tarifaId;

    // --- Especificaciones ---

    @NotBlank
    private String claseCabina;

    @NotNull
    @Min(1)
    private Integer cantidadAdultos;

    @NotNull
    @Min(0)
    private Integer cantidadNinos;

    @NotNull
    @Min(0)
    private Integer cantidadBebes;
}