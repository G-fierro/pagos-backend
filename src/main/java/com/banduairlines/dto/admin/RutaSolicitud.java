package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RutaSolicitud(

        @NotNull(message = "El aeropuerto de origen es obligatorio") 
        @Positive(message = "El identificador de origen debe ser positivo") 
        Long origenId,

        @NotNull(message = "El aeropuerto de destino es obligatorio") 
        @Positive(message = "El identificador de destino debe ser positivo") 
        Long destinoId,

        @Positive(message = "La distancia debe ser mayor que cero") 
        Integer distanciaKm,

        @Positive(message = "La duración debe ser mayor que cero") 
        Integer duracionMin,

        @NotNull(message = "El estado de activación es obligatorio") 
        Boolean activa

) {
}