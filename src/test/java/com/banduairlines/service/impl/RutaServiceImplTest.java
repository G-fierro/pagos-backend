package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Ruta;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.RutaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RutaServiceImplTest {

    @Mock
    private RutaRepository rutas;

    @Mock
    private AeropuertoRepository aeropuertos;

    @InjectMocks
    private RutaServiceImpl servicio;

    @Test
    void actualizarRechazaCambiarRecorridoSiLaRutaTieneVuelos() {
        Aeropuerto origen = Aeropuerto.builder()
            .id(1L)
            .build();

        Aeropuerto destino = Aeropuerto.builder()
            .id(2L)
            .build();

        Ruta ruta = Ruta.builder()
            .id(10L)
            .origen(origen)
            .destino(destino)
            .distanciaKm(620)
            .duracionMin(85)
            .activa(true)
            .build();

        when(rutas.findById(10L))
            .thenReturn(Optional.of(ruta));

        when(rutas.contarVuelosAsociados(10L))
            .thenReturn(1L);

        RutaSolicitud solicitud = new RutaSolicitud(
            1L,
            3L,
            700,
            90,
            true
        );

        assertThatThrownBy(() ->
            servicio.actualizar(10L, solicitud)
        )
            .isInstanceOfSatisfying(
                ConflictoException.class,
                excepcion -> assertThat(excepcion.getCodigo())
                    .isEqualTo("RUTA_CON_VUELOS_ASOCIADOS")
            );

        assertThat(ruta.getOrigen().getId()).isEqualTo(1L);
        assertThat(ruta.getDestino().getId()).isEqualTo(2L);
        assertThat(ruta.getDistanciaKm()).isEqualTo(620);
        assertThat(ruta.getDuracionMin()).isEqualTo(85);

        verify(rutas, never()).saveAndFlush(any(Ruta.class));
        verifyNoInteractions(aeropuertos);
    }
}