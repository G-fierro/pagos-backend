package com.banduairlines.service.impl;

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.reserva.IniciarPagoRequest;
import com.banduairlines.dto.reserva.PagoIniciadoResponse;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Boleto;
import com.banduairlines.model.BoletoExtra;
import com.banduairlines.model.Promocion;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.service.reserva.CalculadoraTotalReserva;
import com.banduairlines.service.reserva.CobroReservaService;
import com.banduairlines.service.reserva.ReservaAcceso;
import com.banduairlines.service.reserva.ReservaPagoService;

@Service
public class ReservaPagoServiceImpl implements ReservaPagoService {

    static final String PRECIO_CAMBIADO = "PRECIO_CAMBIADO";
    static final String SIN_DISPONIBILIDAD = "SIN_DISPONIBILIDAD";
    static final String DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS";

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservaAcceso reservaAcceso;
    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;
    private final PasajeroRepository pasajeroRepository;
    private final CalculadoraTotalReserva calculadora;
    private final CobroReservaService cobroReservaService;
    private final long minutosBloqueo;

    public ReservaPagoServiceImpl(ReservaAcceso reservaAcceso, ReservaRepository reservaRepository,
                                  BoletoRepository boletoRepository, PasajeroRepository pasajeroRepository,
                                  CalculadoraTotalReserva calculadora, CobroReservaService cobroReservaService,
                                  @Value("${app.reserva.minutos-bloqueo:15}") long minutosBloqueo) {
        this.reservaAcceso = reservaAcceso;
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.calculadora = calculadora;
        this.cobroReservaService = cobroReservaService;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Las validaciones que no escriben nada van primero. PRECIO_CAMBIADO no revierte la transacción
     * a propósito: deja guardados los precios vigentes para que GET /reservas/{id} muestre el total
     * nuevo y la segunda llamada (con ese total) pase.
     */
    @Override
    @Transactional(noRollbackFor = ConflictoException.class)
    public PagoIniciadoResponse iniciarPago(Long reservaId, String compraToken, IniciarPagoRequest request) {
        Reserva reserva = reservaAcceso.obtenerAutorizada(reservaId, compraToken);
        if (!ReservaAcceso.ABIERTAS.contains(reserva.getEstado())) {
            throw new ConflictoException("La reserva está " + reserva.getEstado() + " y no se puede pagar")
                    .conCodigo(ReservaAcceso.ESTADO_INVALIDO);
        }
        reservaAcceso.exigirNoExpirada(reserva);

        List<Boleto> boletos = boletoRepository.findByReserva_IdOrderByOrdenTramoAscIdAsc(reserva.getId());
        exigirDatosCompletos(reserva, boletos);
        exigirDisponibilidad(reserva, boletos);

        Promocion promocion = reserva.getPromocion();
        if (promocion != null && !calculadora.promocionVigente(promocion, reservaAcceso.ahora())) {
            promocion = null;
        }
        CalculadoraTotalReserva.Resultado vigente = calculadora.calcular(boletos, promocion, true);

        if (vigente.total() != request.totalEsperado()) {
            actualizarPrecios(reserva, boletos, promocion, vigente);
            throw new ConflictoException("El precio de tu vuelo cambió. Total anterior: " + request.totalEsperado()
                    + " CLP. Total nuevo: " + vigente.total() + " CLP.")
                    .conCodigo(PRECIO_CAMBIADO);
        }
        actualizarPrecios(reserva, boletos, promocion, vigente);

        if (reserva.getCodigoReserva() == null || reserva.getCodigoReserva().isBlank()) {
            reserva.setCodigoReserva(generarCodigoReserva());
        }
        if (reserva.getEstado() == EstadoReserva.EN_CURSO) {
            reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
            reserva.setExpiraEn(reservaAcceso.ahora().plusMinutes(minutosBloqueo));
        }
        reservaRepository.save(reserva);

        CobroReservaService.CobroIniciado cobro =
                cobroReservaService.iniciarCobro(reserva, vigente.total(), reserva.getEmailContacto());

        return new PagoIniciadoResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEstado(),
                cobro.pagoId(),
                cobro.urlPago(),
                vigente.total(),
                reserva.getExpiraEn() != null ? reserva.getExpiraEn().atOffset(ZoneOffset.UTC) : null);
    }

    private void exigirDatosCompletos(Reserva reserva, List<Boleto> boletos) {
        if (boletos.isEmpty() || pasajeroRepository.countByReserva_Id(reserva.getId()) == 0) {
            throw new ReglaNegocioException("Faltan los datos de los pasajeros").conCodigo(DATOS_INCOMPLETOS);
        }
        if (reserva.getEmailContacto() == null || reserva.getEmailContacto().isBlank()) {
            throw new ReglaNegocioException("Faltan los datos de contacto").conCodigo(DATOS_INCOMPLETOS);
        }
    }

    private void exigirDisponibilidad(Reserva reserva, List<Boleto> boletos) {
        for (Boleto b : boletos) {
            if (b.getVueloId().getEstado() == EstadoVuelo.CANCELADO) {
                throw new ConflictoException("El vuelo " + b.getVueloId().getCodigoVuelo() + " fue cancelado")
                        .conCodigo(SIN_DISPONIBILIDAD);
            }
            if (b.getAsientoId() != null && boletoRepository.asientoOcupadoPorOtraReserva(
                    b.getVueloId().getId(), b.getAsientoId().getId(), reserva.getId(), ocupantes())) {
                throw new ConflictoException("El asiento " + b.getAsientoId().getFila() + b.getAsientoId().getLetra()
                        + " del vuelo " + b.getVueloId().getCodigoVuelo() + " ya no está disponible")
                        .conCodigo(SIN_DISPONIBILIDAD);
            }
        }
    }

    /** Estados de otras reservas que mantienen un asiento tomado. */
    private static List<EstadoReserva> ocupantes() {
        return List.of(EstadoReserva.EN_CURSO, EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA);
    }

    /** Guarda en la reserva los precios vigentes con que se va a cobrar. */
    private void actualizarPrecios(Reserva reserva, List<Boleto> boletos, Promocion promocion,
                                   CalculadoraTotalReserva.Resultado vigente) {
        for (Boleto b : boletos) {
            b.setPrecioTarifa(b.getTarifaVueloId().getPrecio());
            for (BoletoExtra be : b.getExtras()) {
                be.setPrecioUnitario(be.getExtra().getPrecio());
            }
        }
        reserva.setPromocion(promocion);
        calculadora.aplicar(reserva, vigente);
        reservaRepository.save(reserva);
    }

    private String generarCodigoReserva() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES_CODIGO.charAt(RANDOM.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsByCodigoReserva(codigo));
        return codigo;
    }
}
