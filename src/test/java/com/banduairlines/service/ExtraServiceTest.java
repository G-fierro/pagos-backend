package com.banduairlines.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banduairlines.dto.admin.ExtraRespuesta;
import com.banduairlines.dto.admin.ExtraSolicitud;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Extra;
import com.banduairlines.model.enums.TipoExtra;
import com.banduairlines.repository.ExtraRepository;
import com.banduairlines.service.impl.ExtraServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExtraServiceTest {

    @Mock
    private ExtraRepository extraRepository;

    @InjectMocks
    private ExtraServiceImpl extraService;

    private ExtraSolicitud solicitud() {
        return new ExtraSolicitud(
                TipoExtra.EMBARQUE_PRIORITARIO,
                "Embarque Prioritario",
                null,
                new BigDecimal("5000.00")
        );
    }

    @Test
    void crear_cuandoValido_guardaYRetorna() {
        when(extraRepository.save(any(Extra.class))).thenAnswer(inv -> {
            Extra extra = inv.getArgument(0);
            extra.setId(1L);
            return extra;
        });

        ExtraRespuesta respuesta = extraService.crear(solicitud());

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.tipo()).isEqualTo(TipoExtra.EMBARQUE_PRIORITARIO);
        assertThat(respuesta.nombre()).isEqualTo("Embarque Prioritario");
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void obtener_cuandoNoExiste_lanzaRecursoNoEncontrado() {
        when(extraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extraService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizar_cuandoNoExiste_lanzaRecursoNoEncontrado() {
        when(extraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extraService.actualizar(99L, solicitud()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_cuandoExiste_actualizaActivo() {
        Extra extra = Extra.builder()
                .id(1L)
                .tipo(TipoExtra.EMBARQUE_PRIORITARIO)
                .nombre("Embarque Prioritario")
                .precio(new BigDecimal("5000.00"))
                .activo(true)
                .build();
        when(extraRepository.findById(1L)).thenReturn(Optional.of(extra));

        ExtraRespuesta respuesta = extraService.cambiarEstado(1L, false);

        assertThat(respuesta.activo()).isFalse();
        assertThat(extra.getActivo()).isFalse();
    }
}
