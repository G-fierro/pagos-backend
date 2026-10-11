package com.banduairlines.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.integra.AeropuertoRequest;
import com.banduairlines.dto.integra.AeropuertoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Aeropuerto;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.service.impl.AeropuertoServiceImpl;

/**
 * Ejemplo de referencia de INTEGRACIÓN; eliminar antes de producción.
 */
@ExtendWith(MockitoExtension.class)
class AeropuertoServiceTest {

    @Mock
    private AeropuertoRepository aeropuertoRepository;

    @InjectMocks
    private AeropuertoServiceImpl aeropuertoService;

    @Test
    void obtenerPorId_cuandoExiste_retornaDto() {
        Aeropuerto aeropuerto = Aeropuerto.builder()
                .id(1L)
                .codigoIata("SCL")
                .nombre("Arturo Merino Benítez")
                .ciudad("Santiago")
                .pais("Chile")
                .zonaHoraria("America/Santiago")
                .build();

        when(aeropuertoRepository.findById(1L)).thenReturn(Optional.of(aeropuerto));

        AeropuertoResponse response = aeropuertoService.obtenerPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.codigoIata()).isEqualTo("SCL");
        assertThat(response.nombre()).isEqualTo("Arturo Merino Benítez");
        verify(aeropuertoRepository).findById(1L);
    }

    @Test
    void obtenerPorId_cuandoNoExiste_lanzaRecursoNoEncontradoException() {
        when(aeropuertoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aeropuertoService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Aeropuerto no encontrado: 99");

        verify(aeropuertoRepository).findById(99L);
    }

    @Test
    void crear_cuandoNoExiste_guardaYRetornaDto() {
        AeropuertoRequest request = new AeropuertoRequest(
                "SCL", "Arturo Merino Benítez", "Santiago", "Chile", "America/Santiago"
        );

        when(aeropuertoRepository.existsByCodigoIata("SCL")).thenReturn(false);
        when(aeropuertoRepository.save(any(Aeropuerto.class))).thenAnswer(invocation -> {
            Aeropuerto guardado = invocation.getArgument(0);
            guardado.setId(10L);
            return guardado;
        });

        AeropuertoResponse response = aeropuertoService.crear(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.codigoIata()).isEqualTo("SCL");
        verify(aeropuertoRepository).save(any(Aeropuerto.class));
    }

    @Test
    void crear_cuandoCodigoIataYaExiste_lanzaConflictoException() {
        AeropuertoRequest request = new AeropuertoRequest(
                "SCL", "Arturo Merino Benítez", "Santiago", "Chile", "America/Santiago"
        );

        when(aeropuertoRepository.existsByCodigoIata("SCL")).thenReturn(true);

        assertThatThrownBy(() -> aeropuertoService.crear(request))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe un aeropuerto con el código IATA: SCL");

        verify(aeropuertoRepository, never()).save(any());
    }
}
