package com.banduairlines.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.banduairlines.dto.admin.PromocionRespuesta;
import com.banduairlines.dto.admin.PromocionSolicitud;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.enums.TipoDescuento;
import com.banduairlines.repository.PromocionRepository;
import com.banduairlines.service.impl.PromocionServiceImpl;

@ExtendWith(MockitoExtension.class)
class PromocionServiceTest {

    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    private PromocionSolicitud solicitud() {
        return new PromocionSolicitud(
                "DESC10",
                TipoDescuento.PORCENTAJE,
                new BigDecimal("10.00"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10),
                null
        );
    }

    @Test
    void crear_cuandoCodigoYaExiste_lanzaConflicto() {
        when(promocionRepository.existsByCodigo("DESC10")).thenReturn(true);

        assertThatThrownBy(() -> promocionService.crear(solicitud()))
                .isInstanceOf(ConflictoException.class);

        verify(promocionRepository, never()).save(any());
    }

    @Test
    void crear_cuandoVigenciaInvalida_lanzaReglaNegocio() {
        when(promocionRepository.existsByCodigo("DESC10")).thenReturn(false);

        PromocionSolicitud invalida = new PromocionSolicitud(
                "DESC10",
                TipoDescuento.PORCENTAJE,
                new BigDecimal("10.00"),
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(1),
                null
        );

        assertThatThrownBy(() -> promocionService.crear(invalida))
                .isInstanceOf(ReglaNegocioException.class);

        verify(promocionRepository, never()).save(any());
    }

    @Test
    void crear_cuandoValido_guardaYRetorna() {
        when(promocionRepository.existsByCodigo("DESC10")).thenReturn(false);
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> {
            Promocion p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PromocionRespuesta respuesta = promocionService.crear(solicitud());

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.codigo()).isEqualTo("DESC10");
        assertThat(respuesta.usosActuales()).isEqualTo(0);
        assertThat(respuesta.activa()).isTrue();
    }

    @Test
    void obtener_cuandoNoExiste_lanzaRecursoNoEncontrado() {
        when(promocionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promocionService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_cuandoExiste_actualizaActiva() {
        Promocion promocion = Promocion.builder()
                .id(1L)
                .codigo("DESC10")
                .tipoDescuento(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10.00"))
                .vigenteDesde(LocalDateTime.now().plusDays(1))
                .vigenteHasta(LocalDateTime.now().plusDays(10))
                .usosActuales(0)
                .activa(true)
                .build();
        when(promocionRepository.findById(1L)).thenReturn(Optional.of(promocion));

        PromocionRespuesta respuesta = promocionService.cambiarEstado(1L, false);

        assertThat(respuesta.activa()).isFalse();
        assertThat(promocion.getActiva()).isFalse();
    }
}
