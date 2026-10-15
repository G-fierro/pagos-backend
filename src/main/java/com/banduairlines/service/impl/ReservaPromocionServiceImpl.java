package com.banduairlines.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.AplicarPromocionRequest;
import com.banduairlines.dto.reserva.PromocionAplicadaResponse;
import com.banduairlines.dto.reserva.TotalReservaResponse;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PromocionRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaPromocionService;
@Service
@Transactional
public class ReservaPromocionServiceImpl implements ReservaPromocionService {

    static final String PROMOCION_INVALIDA = "PROMOCION_INVALIDA";
    static final String PROMOCION_VENCIDA = "PROMOCION_VENCIDA";
    static final String PROMOCION_NO_APLICA = "PROMOCION_NO_APLICA";

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;
    private final PromocionRepository promocionRepository;
    private final CalculadoraTotalReserva calculadora;

    public ReservaPromocionServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                       BoletoRepository boletoRepository, PromocionRepository promocionRepository,
                                       CalculadoraTotalReserva calculadora) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
        this.promocionRepository = promocionRepository;
        this.calculadora = calculadora;
    }

    @Override
    public PromocionAplicadaResponse aplicar(Long reservaId, String compraToken, AplicarPromocionRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        String codigo = request.codigo().trim();
        Promocion promocion = promocionRepository.findByCodigoIgnoreCase(codigo)
                .filter(p -> Boolean.TRUE.equals(p.getActiva()))
                .orElseThrow(() -> new RecursoNoEncontradoException("El código promocional " + codigo + " no existe")
                        .conCodigo(PROMOCION_INVALIDA));

        LocalDateTime ahora = reservaAcceso.ahora();
        if (ahora.isBefore(promocion.getVigenteDesde()) || ahora.isAfter(promocion.getVigenteHasta())) {
            throw new ReglaNegocioException("El código promocional " + promocion.getCodigo() + " no está vigente")
                    .conCodigo(PROMOCION_VENCIDA);
        }
        if (promocion.getUsosMaximos() != null && promocion.getUsosActuales() >= promocion.getUsosMaximos()) {
            throw new ReglaNegocioException("El código promocional " + promocion.getCodigo() + " ya no tiene usos disponibles")
                    .conCodigo(PROMOCION_NO_APLICA);
        }

        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, promocion, false);
        if (total.descuento() <= 0) {
            throw new ReglaNegocioException("El código promocional " + promocion.getCodigo() + " no aplica a esta compra")
                    .conCodigo(PROMOCION_NO_APLICA);
        }

        // Un solo código por reserva: aplicar otro reemplaza al anterior.
        reserva.setPromocion(promocion);
        calculadora.aplicar(reserva, total);
        reservaRepository.save(reserva);

        return new PromocionAplicadaResponse(
                new PromocionAplicadaResponse.Promocion(
                        promocion.getCodigo(),
                        promocion.getTipoDescuento().name(),
                        CalculadoraTotalReserva.clp(promocion.getValor()),
                        total.descuento(),
                        promocion.getVigenteHasta().atOffset(ZoneOffset.UTC)),
                total.aResponse());
    }

    @Override
    public TotalReservaResponse quitar(Long reservaId, String compraToken) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        reservaAcceso.exigirEnCurso(reserva);

        reserva.setPromocion(null);
        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        CalculadoraTotalReserva.Resultado total = calculadora.calcular(boletos, null, false);
        calculadora.aplicar(reserva, total);
        reservaRepository.save(reserva);

        return new TotalReservaResponse(total.aResponse());
    }
}
