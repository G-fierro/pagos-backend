package com.banduairlines.service.impl;

import com.banduairlines.dto.admin.RutaSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Ruta;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.RutaRepository;
import com.banduairlines.model.enums.EstadoVuelo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.banduairlines.exception.ReglaNegocioException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                                true);

                assertThatThrownBy(() -> servicio.actualizar(10L, solicitud))
                                .isInstanceOfSatisfying(
                                                ConflictoException.class,
                                                excepcion -> assertThat(excepcion.getCodigo())
                                                                .isEqualTo("RUTA_CON_VUELOS_ASOCIADOS"));

                assertThat(ruta.getOrigen().getId()).isEqualTo(1L);
                assertThat(ruta.getDestino().getId()).isEqualTo(2L);
                assertThat(ruta.getDistanciaKm()).isEqualTo(620);
                assertThat(ruta.getDuracionMin()).isEqualTo(85);

                verify(rutas, never()).saveAndFlush(any(Ruta.class));
                verifyNoInteractions(aeropuertos);
        }

        @Test
        void actualizarPermiteModificarDatosSinCambiarRecorrido() {
                Aeropuerto origen = Aeropuerto.builder()
                                .id(1L)
                                .codigoIata("SCL")
                                .ciudad("Santiago")
                                .build();

                Aeropuerto destino = Aeropuerto.builder()
                                .id(2L)
                                .codigoIata("ZCO")
                                .ciudad("Temuco")
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

                when(aeropuertos.findById(1L))
                                .thenReturn(Optional.of(origen));

                when(aeropuertos.findById(2L))
                                .thenReturn(Optional.of(destino));

                when(rutas.saveAndFlush(any(Ruta.class)))
                                .thenReturn(ruta);

                RutaSolicitud solicitud = new RutaSolicitud(
                                1L,
                                2L,
                                630,
                                90,
                                true);

                var respuesta = servicio.actualizar(10L, solicitud);

                assertThat(respuesta.id()).isEqualTo(10L);
                assertThat(respuesta.origenId()).isEqualTo(1L);
                assertThat(respuesta.destinoId()).isEqualTo(2L);
                assertThat(respuesta.distanciaKm()).isEqualTo(630);
                assertThat(respuesta.duracionMin()).isEqualTo(90);

                verify(rutas).contarDuplicadasAdministracion(1L, 2L, 10L);
                verify(rutas).saveAndFlush(ruta);

                // Si el recorrido no cambia, no hace falta contar los vuelos.
                verify(rutas, never()).contarVuelosAsociados(any());
        }

        @Test
        void actualizarPermiteCambiarRecorridoSiNoHayVuelos() {
                Aeropuerto origen = Aeropuerto.builder()
                                .id(1L)
                                .codigoIata("SCL")
                                .ciudad("Santiago")
                                .build();

                Aeropuerto destinoAnterior = Aeropuerto.builder()
                                .id(2L)
                                .build();

                Aeropuerto destinoNuevo = Aeropuerto.builder()
                                .id(3L)
                                .codigoIata("CCP")
                                .ciudad("Concepción")
                                .build();

                Ruta ruta = Ruta.builder()
                                .id(10L)
                                .origen(origen)
                                .destino(destinoAnterior)
                                .distanciaKm(620)
                                .duracionMin(85)
                                .activa(true)
                                .build();

                when(rutas.findById(10L))
                                .thenReturn(Optional.of(ruta));

                when(rutas.contarVuelosAsociados(10L))
                                .thenReturn(0L);

                when(aeropuertos.findById(1L))
                                .thenReturn(Optional.of(origen));

                when(aeropuertos.findById(3L))
                                .thenReturn(Optional.of(destinoNuevo));

                when(rutas.saveAndFlush(any(Ruta.class)))
                                .thenReturn(ruta);

                RutaSolicitud solicitud = new RutaSolicitud(
                                1L,
                                3L,
                                430,
                                60,
                                true);

                var respuesta = servicio.actualizar(10L, solicitud);

                assertThat(respuesta.origenId()).isEqualTo(1L);
                assertThat(respuesta.destinoId()).isEqualTo(3L);
                assertThat(respuesta.destinoCodigoIata()).isEqualTo("CCP");
                assertThat(respuesta.distanciaKm()).isEqualTo(430);
                assertThat(respuesta.duracionMin()).isEqualTo(60);

                verify(rutas).contarVuelosAsociados(10L);
                verify(rutas).contarDuplicadasAdministracion(1L, 3L, 10L);
                verify(rutas).saveAndFlush(ruta);
        }

        @Test
        void crearRechazaOrigenIgualAlDestino() {
                RutaSolicitud solicitud = new RutaSolicitud(
                                1L,
                                1L,
                                100,
                                60,
                                true);

                assertThatThrownBy(() -> servicio.crear(solicitud))
                                .isInstanceOf(ReglaNegocioException.class);

                verify(rutas, never()).saveAndFlush(any(Ruta.class));
                verifyNoInteractions(aeropuertos);
        }

        @Test
        void crearRechazaRutaDuplicada() {
                when(rutas.contarDuplicadasAdministracion(1L, 2L, null))
                                .thenReturn(1L);

                RutaSolicitud solicitud = new RutaSolicitud(
                                1L,
                                2L,
                                620,
                                85,
                                true);

                assertThatThrownBy(() -> servicio.crear(solicitud))
                                .isInstanceOf(ConflictoException.class);

                verify(rutas, never()).saveAndFlush(any(Ruta.class));
                verifyNoInteractions(aeropuertos);
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void cambiarEstadoConservaElRecorrido(boolean activa) {
                Aeropuerto origen = Aeropuerto.builder()
                                .id(1L)
                                .codigoIata("SCL")
                                .ciudad("Santiago")
                                .build();

                Aeropuerto destino = Aeropuerto.builder()
                                .id(2L)
                                .codigoIata("ZCO")
                                .ciudad("Temuco")
                                .build();

                Ruta ruta = Ruta.builder()
                                .id(10L)
                                .origen(origen)
                                .destino(destino)
                                .distanciaKm(620)
                                .duracionMin(85)
                                .activa(!activa)
                                .build();

                when(rutas.findById(10L))
                                .thenReturn(Optional.of(ruta));

                when(rutas.saveAndFlush(any(Ruta.class)))
                                .thenReturn(ruta);

                var respuesta = servicio.cambiarEstado(10L, activa);

                assertThat(respuesta.activa()).isEqualTo(activa);
                assertThat(respuesta.origenId()).isEqualTo(1L);
                assertThat(respuesta.destinoId()).isEqualTo(2L);
                assertThat(respuesta.distanciaKm()).isEqualTo(620);
                assertThat(respuesta.duracionMin()).isEqualTo(85);

                verify(rutas).saveAndFlush(ruta);
                verifyNoInteractions(aeropuertos);
        }

        @Test
        void cambiarEstadoRechazaDesactivarRutaConVuelosProgramados() {
                Ruta ruta = crearRutaActivaParaPrueba();

                when(rutas.findById(10L))
                                .thenReturn(Optional.of(ruta));

                when(rutas.contarVuelosPorEstado(10L, EstadoVuelo.PROGRAMADO))
                                .thenReturn(1L);

                assertThatThrownBy(() -> servicio.cambiarEstado(10L, false))
                                .isInstanceOfSatisfying(
                                                ConflictoException.class,
                                                excepcion -> assertThat(excepcion.getCodigo())
                                                                .isEqualTo("RUTA_CON_VUELOS_PROGRAMADOS"));

                assertThat(ruta.getActiva()).isTrue();

                verify(rutas, never()).saveAndFlush(any(Ruta.class));
                verifyNoInteractions(aeropuertos);
        }

        @Test
        void actualizarRechazaDesactivarRutaConVuelosProgramados() {
                Ruta ruta = crearRutaActivaParaPrueba();

                when(rutas.findById(10L))
                                .thenReturn(Optional.of(ruta));

                when(rutas.contarVuelosPorEstado(10L, EstadoVuelo.PROGRAMADO))
                                .thenReturn(1L);

                RutaSolicitud solicitud = new RutaSolicitud(
                                1L, 2L, 630, 90, false);

                assertThatThrownBy(() -> servicio.actualizar(10L, solicitud))
                                .isInstanceOfSatisfying(
                                                ConflictoException.class,
                                                excepcion -> assertThat(excepcion.getCodigo())
                                                                .isEqualTo("RUTA_CON_VUELOS_PROGRAMADOS"));

                assertThat(ruta.getActiva()).isTrue();
                assertThat(ruta.getDistanciaKm()).isEqualTo(620);
                assertThat(ruta.getDuracionMin()).isEqualTo(85);

                verify(rutas, never()).saveAndFlush(any(Ruta.class));
                verifyNoInteractions(aeropuertos);
        }

        private Ruta crearRutaActivaParaPrueba() {
                Aeropuerto origen = Aeropuerto.builder()
                                .id(1L)
                                .build();

                Aeropuerto destino = Aeropuerto.builder()
                                .id(2L)
                                .build();

                return Ruta.builder()
                                .id(10L)
                                .origen(origen)
                                .destino(destino)
                                .distanciaKm(620)
                                .duracionMin(85)
                                .activa(true)
                                .build();
        }
}