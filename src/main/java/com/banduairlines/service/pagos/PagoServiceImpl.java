package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import com.banduairlines.model.Pago;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");
    private static final DateTimeFormatter FECHA_FLOW = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long BASE_NUMERO_BOLETA = 2_000_000L;

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaConsultaInterna reservaConsultaInterna;
    private final ReservaAcceso reservaAcceso;
    private final FlowClient flowClient;
    private final ReservasNotificationClient reservasNotificationClient;
    private final BoletaPdfGenerator boletaPdfGenerator;
    private final UsuarioActual usuarioActual;
    private final ObjectMapper objectMapper;

    @Value("${bandu.internas.service-key:clave_compartida_reservas}")
    private String configuredServiceKey;

    @Value("${bandu.flow.url-confirmacion:${FLOW_URL_CONFIRMACION:http://localhost:8080/api/v1/pagos/notificaciones}}")
    private String flowUrlConfirmacion;

    @Value("${bandu.flow.url-retorno:${FLOW_URL_RETORNO:http://localhost:8080/api/v1/pagos/retorno}}")
    private String flowUrlRetorno;

    @Value("${bandu.flow.url-reembolso-callback:${FLOW_URL_REEMBOLSO_CALLBACK:http://localhost:8080/api/v1/pagos/reembolsos/notificaciones}}")
    private String flowUrlReembolsoCallback;

    @Value("${bandu.frontend.url:${FRONTEND_URL:}}")
    private String frontendUrl;

    @Value("${bandu.flow.api-url:https://sandbox.flow.cl/api}")
    private String flowApiUrl;

    public PagoServiceImpl(
            PagoRepository pagoRepository,
            ReservaRepository reservaRepository,
            ReservaConsultaInterna reservaConsultaInterna,
            ReservaAcceso reservaAcceso,
            FlowClient flowClient,
            ReservasNotificationClient reservasNotificationClient,
            BoletaPdfGenerator boletaPdfGenerator,
            UsuarioActual usuarioActual
    ) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.reservaConsultaInterna = reservaConsultaInterna;
        this.reservaAcceso = reservaAcceso;
        this.flowClient = flowClient;
        this.reservasNotificationClient = reservasNotificationClient;
        this.boletaPdfGenerator = boletaPdfGenerator;
        this.usuarioActual = usuarioActual;
        // Mapper propio de Jackson 2: Spring Boot 4 solo expone un ObjectMapper de Jackson 3 como bean.
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public PagoIniciadoResponse iniciarPago(IniciarPagoRequest request, String serviceKey) {
        validarServiceKey(serviceKey);

        Long reservaId = request.reservaId();
        ReservaParaPago reservaInfo = reservaConsultaInterna.obtenerParaPago(reservaId);

        if (reservaInfo.estado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ConflictoException("La reserva no está en PENDIENTE_PAGO (estado actual: " + reservaInfo.estado() + ")")
                    .conCodigo("ESTADO_INVALIDO");
        }

        Optional<Pago> pagoAprobadoExistente = pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(reservaId, EstadoPago.PAGADO);
        if (pagoAprobadoExistente.isPresent()) {
            throw new ConflictoException("La reserva ya tiene un pago aprobado")
                    .conCodigo("PAGO_YA_PAGADO");
        }

        int totalCLP = reservaInfo.total().totalCLP();
        if (totalCLP <= 0) {
            throw new ReglaNegocioException("El total de la reserva es menor o igual a cero")
                    .conCodigo("MONTO_INVALIDO");
        }

        List<Pago> pagosAnteriores = pagoRepository.findByReservaIdOrderByCreatedAtDesc(reservaId);
        Instant ahora = Instant.now();

        if (!pagosAnteriores.isEmpty()) {
            Pago ultimoPago = pagosAnteriores.get(0);
            if (ultimoPago.getEstado() == EstadoPago.PENDIENTE) {
                boolean vigenciaValida = reservaInfo.expiraEn().isAfter(ahora);
                if (vigenciaValida && ultimoPago.getFlowToken() != null && !esSinReserva(ultimoPago)) {
                    String urlPago = armarUrlPago(ultimoPago.getFlowToken());
                    return new PagoIniciadoResponse(
                            ultimoPago.getId(),
                            reservaInfo.codigoReserva(),
                            EstadoPago.PENDIENTE,
                            urlPago,
                            ultimoPago.getMonto().intValue(),
                            reservaInfo.expiraEn()
                    );
                } else if (!vigenciaValida && !esSinReserva(ultimoPago)) {
                    ultimoPago.setEstado(EstadoPago.RECHAZADO);
                    agregarADetalleRespuesta(ultimoPago, "motivo", "VENCIDO");
                    pagoRepository.save(ultimoPago);
                }
            }
        }

        // La orden de Flow debe vencer junto con la reserva: Pagos no extiende expiraEn.
        long timeoutSeconds = Duration.between(ahora, reservaInfo.expiraEn()).getSeconds();
        if (timeoutSeconds <= 0) {
            throw new ConflictoException("La reserva ya venció")
                    .conCodigo("ESTADO_INVALIDO");
        }

        Reserva reservaEntity = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        Pago nuevoPago = Pago.builder()
                .reserva(reservaEntity)
                .monto(BigDecimal.valueOf(totalCLP))
                .moneda("CLP")
                .estado(EstadoPago.PENDIENTE)
                .build();

        Pago pagoGuardado = pagoRepository.save(nuevoPago);

        String commerceOrder = "PAGO-" + pagoGuardado.getId();

        FlowPaymentCreateResponse flowResponse = flowClient.crearPago(
                commerceOrder,
                "Ticket de avión",
                totalCLP,
                reservaInfo.contacto().correo(),
                flowUrlConfirmacion,
                flowUrlRetorno,
                timeoutSeconds
        );

        pagoGuardado.setFlowToken(flowResponse.token());
        pagoGuardado.setFlowOrder(flowResponse.flowOrder());
        pagoRepository.save(pagoGuardado);

        String urlPagoFinal = flowResponse.url() != null && flowResponse.url().contains("?token=")
                ? flowResponse.url()
                : armarUrlPago(flowResponse.token());

        return new PagoIniciadoResponse(
                pagoGuardado.getId(),
                reservaInfo.codigoReserva(),
                EstadoPago.PENDIENTE,
                urlPagoFinal,
                totalCLP,
                reservaInfo.expiraEn()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagoDetalleResponse obtenerPago(Long pagoId, String compraToken) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago no existe: " + pagoId)
                        .conCodigo("PAGO_NO_ENCONTRADO"));

        reservaAcceso.obtenerAutorizada(pago.getReserva().getId(), compraToken);

        boolean puedeReintentar = false;
        if (pago.getEstado() == EstadoPago.RECHAZADO && !esSinReserva(pago)) {
            LocalDateTime expiraEn = pago.getReserva().getExpiraEn();
            if (expiraEn != null && expiraEn.isAfter(ahoraUtc())) {
                puedeReintentar = true;
            }
        }

        MedioPagoDto medioPago = extraerMedioPago(pago);
        String motivoRechazo = extraerMotivoRechazo(pago);
        Long boletaId = pago.getEstado() == EstadoPago.PAGADO && tieneBoleta(pago) ? pago.getId() : null;

        Instant fechaPagoInstant = pago.getFechaPago() != null
                ? pago.getFechaPago().toInstant(ZoneOffset.UTC)
                : null;

        return new PagoDetalleResponse(
                pago.getId(),
                pago.getReserva().getId(),
                pago.getReserva().getCodigoReserva(),
                pago.getEstado(),
                pago.getMonto().intValue(),
                medioPago,
                pago.getFlowOrder(),
                fechaPagoInstant,
                motivoRechazo,
                boletaId,
                puedeReintentar
        );
    }

    @Override
    public void procesarNotificacionFlow(String token) {
        if (token == null || token.isBlank()) {
            throw violacion("Falta el token de Flow");
        }

        Pago pago = pagoRepository.findByFlowToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe pago asociado al token de Flow")
                        .conCodigo("PAGO_NO_ENCONTRADO"));

        if (esSinReserva(pago)) {
            log.info("Pago {} ya fue marcado sin reserva y enviado a devolución; se ignora el aviso", pago.getId());
            return;
        }
        if (pago.getEstado() == EstadoPago.PAGADO && tieneBoleta(pago)) {
            log.info("Notificación ya procesada con éxito anteriormente para pagoId={}", pago.getId());
            return;
        }

        FlowPaymentStatusResponse flowStatus = flowClient.obtenerEstadoPago(token);
        EstadoPago nuevoEstado = mapearEstadoFlow(flowStatus.status());
        int montoPago = pago.getMonto().intValue();

        // Flow cobró un monto distinto al del pago: no se confirma, se devuelve lo cobrado.
        if (nuevoEstado == EstadoPago.PAGADO && flowStatus.amount() != null && flowStatus.amount().intValue() != montoPago) {
            log.warn("Monto informado por Flow ({}) difiere del pago ({})", flowStatus.amount(), montoPago);
            marcarSinReserva(pago, "MONTO_INVALIDO", flowStatus.amount(), flowStatus.payer());
            pagoRepository.save(pago);
            return;
        }

        if (nuevoEstado == EstadoPago.PENDIENTE) {
            return;
        }
        if (pago.getEstado() == nuevoEstado && nuevoEstado == EstadoPago.RECHAZADO) {
            return;
        }

        // Nada se modifica en el pago hasta que Reservas responda: si falla con 5xx, Flow reintenta.
        LocalDateTime fechaPago = nuevoEstado == EstadoPago.PAGADO ? fechaPagoFlow(flowStatus) : ahoraUtc();
        String medioNombre = nombreMedioPago(flowStatus);
        String ultimosDigitos = ultimosDigitos(flowStatus);
        MedioPagoDto medioDto = medioNombre != null ? new MedioPagoDto(medioNombre, ultimosDigitos) : null;
        Long flowOrder = pago.getFlowOrder() != null ? pago.getFlowOrder() : flowStatus.flowOrder();
        String motivoRechazo = nuevoEstado == EstadoPago.RECHAZADO
                ? (flowStatus.status() != null && flowStatus.status() == 4
                        ? "Pago anulado o cancelado en Flow" : "Pago rechazado en Flow")
                : null;

        BoletaResponse boleta = null;
        if (nuevoEstado == EstadoPago.PAGADO) {
            ReservaParaPago reservaInfo = reservaConsultaInterna.obtenerParaPago(pago.getReserva().getId());
            boleta = emitirBoleta(pago, reservaInfo, medioDto, flowOrder, fechaPago);
        }

        ResultadoPagoRequest resultadoRequest = new ResultadoPagoRequest(
                pago.getId(),
                flowOrder != null ? flowOrder : pago.getId(),
                nuevoEstado,
                montoPago,
                fechaPago.toInstant(ZoneOffset.UTC),
                motivoRechazo
        );

        ReservasNotificationClient.ReservasNotificacionResult res =
                reservasNotificationClient.notificarResultado(pago.getReserva().getId(), resultadoRequest);

        if (pago.getFlowOrder() == null && flowOrder != null) {
            pago.setFlowOrder(flowOrder);
        }
        if (ultimosDigitos != null) {
            agregarADetalleRespuesta(pago, "ultimosDigitos", ultimosDigitos);
        }
        guardarRespuestaRaw(pago, flowStatus);

        if (!res.exitoso() && esRechazoDeReserva(res.errorCode())) {
            log.warn("Reservas rechazó el resultado con {}. Se devuelve el cobro por Flow.", res.errorCode());
            agregarADetalleRespuesta(pago, "reservasError", res.errorCode());
            int cobrado = flowStatus.amount() != null ? flowStatus.amount() : montoPago;
            marcarSinReserva(pago, res.errorCode(), cobrado, flowStatus.payer());
        } else {
            pago.setEstado(nuevoEstado);
            // fechaPago es el momento en que Flow confirmó el pago: solo existe en pagos PAGADO.
            pago.setFechaPago(nuevoEstado == EstadoPago.PAGADO ? fechaPago : null);
            // medioPago es nulo mientras el pago esté PENDIENTE (incluye los pagos devueltos por Flow).
            if (medioNombre != null) {
                pago.setMedioPago(medioNombre.length() > 50 ? medioNombre.substring(0, 50) : medioNombre);
            }
            if (motivoRechazo != null) {
                agregarADetalleRespuesta(pago, "motivoRechazo", motivoRechazo);
            }
            if (!res.exitoso()) {
                agregarADetalleRespuesta(pago, "reservasError", res.errorCode());
            }
            if (boleta != null) {
                guardarBoletaEnDetalle(pago, boleta);
            }
        }

        pagoRepository.save(pago);
    }

    @Override
    public String procesarRetornoFlow(String token) {
        String baseRedirect = resolverBaseFrontend();
        if (token == null || token.isBlank()) {
            return baseRedirect + "/pago/resultado";
        }
        try {
            Optional<Pago> pagoOpt = pagoRepository.findByFlowToken(token);
            if (pagoOpt.isEmpty()) {
                return baseRedirect + "/pago/resultado";
            }
            procesarNotificacionFlow(token);
            return baseRedirect + "/pago/resultado?pagoId=" + pagoOpt.get().getId();
        } catch (Exception e) {
            log.error("Error al procesar retorno de Flow: {}", e.getMessage(), e);
            Optional<Pago> pagoOpt = pagoRepository.findByFlowToken(token);
            return pagoOpt.map(pago -> baseRedirect + "/pago/resultado?pagoId=" + pago.getId())
                    .orElse(baseRedirect + "/pago/resultado");
        }
    }

    private String resolverBaseFrontend() {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        }
        return "";
    }

    @Override
    public void procesarNotificacionReembolso(String token) {
        if (token == null || token.isBlank()) {
            throw violacion("Falta el token del reembolso");
        }

        Pago pago = pagoRepository.findByReembolsoToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe pago asociado al token de reembolso")
                        .conCodigo("PAGO_NO_ENCONTRADO"));

        FlowRefundResponse refundStatus = flowClient.obtenerEstadoReembolso(token);

        Map<String, Object> reembolso = leerReembolso(pago);
        if (refundStatus != null) {
            reembolso.put("estado", refundStatus.status());
            if (refundStatus.flowRefundOrder() != null) {
                reembolso.put("flowRefundOrder", refundStatus.flowRefundOrder());
            }
        }
        reembolso.put("actualizadoEn", Instant.now().toString());
        agregarADetalleRespuesta(pago, "reembolso", reembolso);
        pagoRepository.save(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse obtenerBoletaPorReserva(Long reservaId, String compraToken) {
        reservaAcceso.obtenerAutorizada(reservaId, compraToken);

        Pago pago = pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(reservaId, EstadoPago.PAGADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe boleta aprobada para la reserva")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        return leerBoletaDesdeDetalle(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<BoletaResumenResponse> obtenerHistorialBoletas(int pagina, int tamano,
                                                                         LocalDate desde, LocalDate hasta,
                                                                         String codigoReserva) {
        UsuarioAutenticado usuario = usuarioActual.exigir();
        if (usuario.rol() != RolUsuario.PASAJERO) {
            throw new AccessDeniedException("Solo pasajeros pueden consultar su historial de boletas");
        }
        validarPaginacion(pagina, tamano);
        validarRango(desde, hasta);

        String codigo = codigoReserva != null && !codigoReserva.isBlank()
                ? codigoReserva.trim().toUpperCase(Locale.ROOT)
                : "%";

        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Pago> pagosPagados = pagoRepository.findPagadosByUsuarioId(
                usuario.id(),
                desde != null ? inicioDiaUtc(desde) : LocalDateTime.of(1970, 1, 1, 0, 0),
                hasta != null ? finDiaUtc(hasta) : LocalDateTime.of(9999, 12, 31, 23, 59, 59),
                codigo,
                pageable);

        // Todos los datos del resumen están en columnas: no hace falta leer el JSON de cada pago.
        List<BoletaResumenResponse> resumenes = pagosPagados.getContent().stream()
                .map(p -> new BoletaResumenResponse(
                        p.getId(),
                        numeroBoleta(p.getId()),
                        p.getReserva().getCodigoReserva(),
                        p.getFechaPago() != null ? p.getFechaPago().toInstant(ZoneOffset.UTC) : null,
                        p.getMonto().intValue()
                ))
                .toList();

        return new PaginaResponse<>(
                resumenes,
                pagosPagados.getNumber(),
                pagosPagados.getSize(),
                pagosPagados.getTotalElements(),
                pagosPagados.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse obtenerBoletaPorId(Long boletaId) {
        Pago pago = pagoRepository.findById(boletaId)
                .filter(p -> p.getEstado() == EstadoPago.PAGADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        UsuarioAutenticado usuario = usuarioActual.exigir();
        if (usuario.rol() != RolUsuario.ADMINISTRADOR) {
            if (pago.getReserva().getUsuario() == null || !pago.getReserva().getUsuario().getId().equals(usuario.id())) {
                throw new AccessDeniedException("No tienes permisos para ver esta boleta");
            }
        }

        return leerBoletaDesdeDetalle(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaPdfDto generarBoletaPdf(Long boletaId, String compraToken) {
        Pago pago = pagoRepository.findById(boletaId)
                .filter(p -> p.getEstado() == EstadoPago.PAGADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        reservaAcceso.obtenerAutorizada(pago.getReserva().getId(), compraToken);
        BoletaResponse boleta = leerBoletaDesdeDetalle(pago);
        return new BoletaPdfDto(boleta.numeroBoleta(), boletaPdfGenerator.generarBoletaPdf(boleta));
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse consultarBoletaInvitado(ConsultaBoletaInvitadoRequest request) {
        Reserva reserva = reservaRepository.findByCodigoReservaIgnoreCase(request.codigoReserva().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        if (reserva.getEmailContacto() == null || !reserva.getEmailContacto().equalsIgnoreCase(request.correo().trim())) {
            throw new RecursoNoEncontradoException("Boleta no encontrada")
                    .conCodigo("BOLETA_NO_ENCONTRADA");
        }

        Pago pago = pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(reserva.getId(), EstadoPago.PAGADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        return leerBoletaDesdeDetalle(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tienePagoEnEspera(Long reservaId) {
        List<Pago> pagos = pagoRepository.findByReservaIdOrderByCreatedAtDesc(reservaId);
        if (pagos.isEmpty()) return false;
        Pago p = pagos.get(0);
        if (p.getEstado() != EstadoPago.PENDIENTE) return false;
        if (esSinReserva(p)) return false;
        LocalDateTime expiraEn = p.getReserva().getExpiraEn();
        return expiraEn == null || expiraEn.isAfter(ahoraUtc());
    }

    // ---------- Métodos Auxiliares ----------

    private void validarServiceKey(String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank() || !serviceKey.equals(configuredServiceKey)) {
            throw new AuthenticationCredentialsNotFoundException("Clave compartida X-Service-Key inválida o ausente");
        }
    }

    /** GlobalExceptionHandler la responde como 400 VALIDACION. */
    private static ConstraintViolationException violacion(String mensaje) {
        return new ConstraintViolationException(mensaje, Set.of());
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw violacion("La página debe ser mayor o igual a 0");
        }
        if (tamano < 1 || tamano > 50) {
            throw violacion("El tamaño de página debe estar entre 1 y 50");
        }
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw violacion("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Inicio del día UTC (las fechas se guardan y se comparan en UTC). */
    private static LocalDateTime inicioDiaUtc(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    /** Último instante del día UTC. */
    private static LocalDateTime finDiaUtc(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay().minusNanos(1);
    }

    private static Long numeroBoleta(Long pagoId) {
        return BASE_NUMERO_BOLETA + pagoId;
    }

    private String armarUrlPago(String token) {
        String base = flowApiUrl.replace("/api", "/app/web/pay.php");
        return base + "?token=" + token;
    }

    private EstadoPago mapearEstadoFlow(Integer status) {
        if (status == null) return EstadoPago.PENDIENTE;
        return switch (status) {
            case 2 -> EstadoPago.PAGADO;
            case 3, 4 -> EstadoPago.RECHAZADO;
            default -> EstadoPago.PENDIENTE;
        };
    }

    private boolean esRechazoDeReserva(String codigo) {
        return "ESTADO_INVALIDO".equals(codigo)
                || "PAGO_YA_PAGADO".equals(codigo)
                || "MONTO_INVALIDO".equals(codigo);
    }

    private String nombreMedioPago(FlowPaymentStatusResponse flowStatus) {
        FlowPaymentStatusResponse.PaymentData data = flowStatus.paymentData();
        if (data == null) return null;
        String brand = data.card() != null ? data.card().brand() : null;
        if (brand != null && !brand.isBlank()) {
            return "Tarjeta de Crédito " + brand;
        }
        return data.media() != null && !data.media().isBlank() ? data.media() : "Flow";
    }

    private String ultimosDigitos(FlowPaymentStatusResponse flowStatus) {
        if (flowStatus.paymentData() == null || flowStatus.paymentData().card() == null) return null;
        String digitos = flowStatus.paymentData().card().last4CardDigits();
        return digitos != null && !digitos.isBlank() ? digitos : null;
    }

    /** Momento en que Flow confirmó el pago (hora de Chile) llevado a UTC; si no viene, el momento actual. */
    private LocalDateTime fechaPagoFlow(FlowPaymentStatusResponse flowStatus) {
        if (flowStatus.paymentData() != null && flowStatus.paymentData().date() != null) {
            try {
                return LocalDateTime.parse(flowStatus.paymentData().date().trim(), FECHA_FLOW)
                        .atZone(ZONA_CHILE)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            } catch (DateTimeParseException e) {
                log.warn("Fecha de pago de Flow con formato inesperado: {}", flowStatus.paymentData().date());
            }
        }
        return ahoraUtc();
    }

    /**
     * Deja el pago PENDIENTE marcado sin reserva y pide la devolución a Flow.
     * Ese pago no cuenta como ingreso ni genera boleta.
     */
    private void marcarSinReserva(Pago pago, String motivo, int montoCobrado, String emailPagador) {
        pago.setEstado(EstadoPago.PENDIENTE);
        agregarADetalleRespuesta(pago, "sinReserva", true);
        agregarADetalleRespuesta(pago, "motivo", motivo);
        ejecutarDevolucionFlow(pago, montoCobrado, emailPagador);
    }

    private void ejecutarDevolucionFlow(Pago pago, int monto, String emailPagador) {
        Map<String, Object> reembolso = new LinkedHashMap<>();
        reembolso.put("monto", monto);
        reembolso.put("solicitadoEn", Instant.now().toString());
        try {
            String receiverEmail = emailPagador != null && !emailPagador.isBlank()
                    ? emailPagador
                    : pago.getReserva().getEmailContacto();
            FlowRefundResponse respuesta = flowClient.crearReembolso(
                    "REF-" + pago.getId(),
                    receiverEmail,
                    monto,
                    flowUrlReembolsoCallback,
                    pago.getFlowOrder()
            );
            reembolso.put("estado", respuesta != null && respuesta.status() != null ? respuesta.status() : "SOLICITADO");
            if (respuesta != null) {
                reembolso.put("token", respuesta.token());
                reembolso.put("flowRefundOrder", respuesta.flowRefundOrder());
            }
        } catch (Exception e) {
            // Queda registrado para que se revise y se reintente manualmente.
            log.error("Error al ejecutar devolución en Flow para pagoId {}: {}", pago.getId(), e.getMessage(), e);
            reembolso.put("estado", "ERROR_SOLICITUD");
            reembolso.put("error", e.getMessage());
        }
        agregarADetalleRespuesta(pago, "reembolso", reembolso);
    }

    private BoletaResponse emitirBoleta(Pago pago, ReservaParaPago reservaInfo, MedioPagoDto medioPago,
                                        Long flowOrder, LocalDateTime fechaPago) {
        List<String> nombresPasajeros = reservaInfo.pasajeros().stream()
                .map(p -> (p.nombres() + " " + p.apellidos()).trim())
                .toList();

        List<LineaBoletaDto> lineasBoleta = reservaInfo.total().lineas().stream()
                .map(l -> new LineaBoletaDto(l.concepto(), l.descripcion(), l.montoCLP()))
                .toList();

        Instant fecha = fechaPago.toInstant(ZoneOffset.UTC);
        PagoBoletaDto pagoBoleta = new PagoBoletaDto(EstadoPago.PAGADO, medioPago, flowOrder, fecha);

        return new BoletaResponse(
                pago.getId(),
                numeroBoleta(pago.getId()),
                fecha,
                "VENTA_WEB",
                pago.getReserva().getId(),
                pago.getReserva().getCodigoReserva(),
                reservaInfo.contacto().nombreCompleto(),
                nombresPasajeros,
                lineasBoleta,
                reservaInfo.total().descuentoCLP(),
                pago.getMonto().intValue(),
                pagoBoleta
        );
    }

    private void guardarBoletaEnDetalle(Pago pago, BoletaResponse boleta) {
        agregarADetalleRespuesta(pago, "boleta", boleta);
    }

    /** La boleta es inmutable: se lee tal como se guardó al cobrar, nunca se reconstruye. */
    private BoletaResponse leerBoletaDesdeDetalle(Pago pago) {
        try {
            Map<String, Object> map = leerDetalleComoMap(pago);
            if (map.containsKey("boleta")) {
                String boletaJson = objectMapper.writeValueAsString(map.get("boleta"));
                return objectMapper.readValue(boletaJson, BoletaResponse.class);
            }
        } catch (Exception e) {
            log.error("No se pudo leer la boleta guardada del pago {}: {}", pago.getId(), e.getMessage(), e);
        }
        log.error("El pago {} está PAGADO pero no tiene boleta guardada", pago.getId());
        throw new RecursoNoEncontradoException("Boleta no encontrada")
                .conCodigo("BOLETA_NO_ENCONTRADA");
    }

    private boolean tieneBoleta(Pago pago) {
        Map<String, Object> map = leerDetalleComoMap(pago);
        return map.containsKey("boleta");
    }

    private boolean esSinReserva(Pago pago) {
        Map<String, Object> map = leerDetalleComoMap(pago);
        return Boolean.TRUE.equals(map.get("sinReserva"));
    }

    private void agregarADetalleRespuesta(Pago pago, String clave, Object valor) {
        try {
            Map<String, Object> map = leerDetalleComoMap(pago);
            map.put(clave, valor);
            pago.setDetalleRespuesta(objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.error("Error al actualizar detalleRespuesta: {}", e.getMessage(), e);
        }
    }

    private void guardarRespuestaRaw(Pago pago, FlowPaymentStatusResponse flowStatus) {
        agregarADetalleRespuesta(pago, "flowResponse", flowStatus);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leerReembolso(Pago pago) {
        Object valor = leerDetalleComoMap(pago).get("reembolso");
        return valor instanceof Map ? new LinkedHashMap<>((Map<String, Object>) valor) : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leerDetalleComoMap(Pago pago) {
        if (pago.getDetalleRespuesta() == null || pago.getDetalleRespuesta().isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(pago.getDetalleRespuesta(), Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private MedioPagoDto extraerMedioPago(Pago pago) {
        if (pago.getEstado() == EstadoPago.PENDIENTE || pago.getMedioPago() == null || pago.getMedioPago().isBlank()) {
            return null;
        }
        Object digitos = leerDetalleComoMap(pago).get("ultimosDigitos");
        return new MedioPagoDto(pago.getMedioPago(), digitos != null ? String.valueOf(digitos) : null);
    }

    private String extraerMotivoRechazo(Pago pago) {
        if (pago.getEstado() != EstadoPago.RECHAZADO) {
            return null;
        }
        Map<String, Object> map = leerDetalleComoMap(pago);
        Object motivo = map.containsKey("motivoRechazo") ? map.get("motivoRechazo") : map.get("motivo");
        return motivo != null ? String.valueOf(motivo) : null;
    }
}
