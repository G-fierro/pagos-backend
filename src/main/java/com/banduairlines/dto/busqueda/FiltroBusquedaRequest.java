package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class FiltroBusquedaRequest {

    // --- Ordenamiento ---

    @Pattern(regexp = "^(precio|horario_salida|duracion|escalas)$", message = "Criterio de ordenamiento inválido")
    private String ordenarPor = "precio";

    @Pattern(regexp = "^(asc|desc)$", message = "Orden debe ser asc o desc")
    private String orden = "asc";

    // --- Filtros ---

    @Min(value = 0, message = "El precio máximo debe ser mayor o igual a 0")
    private BigDecimal precioMaximo;

    private LocalTime horarioSalidaMinimo;

    private LocalTime horarioSalidaMaximo;

    @Min(value = 0, message = "La duración máxima no puede ser negativa")
    private Integer duracionMaximaMinutos;

    @Min(value = 0, message = "El máximo de escalas no puede ser negativo")
    private Integer maxEscalas;
}