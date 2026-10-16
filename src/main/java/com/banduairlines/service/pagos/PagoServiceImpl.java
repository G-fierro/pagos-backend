package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.Pago;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.security.UsuarioAutenticado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.*;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");

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
            UsuarioActual usuarioActual,
            ObjectMapper objectMapper
    ) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.reservaConsultaInterna = reservaConsultaInterna;
        this.reservaAcceso = reservaAcceso;
        this.flowClient = flowClient;
        this.reservasNotificationClient = reservasNotificationClient;
        this.boletaPdfGenerator = boletaPdfGenerator;
        this.usuarioActual = usuarioActual;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
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
                } else if (!vigenciaValida) {
                    ultimoPago.setEstado(EstadoPago.RECHAZADO);
                    agregarADetalleRespuesta(ultimoPago, "motivo", "VENCIDO");
                    pagoRepository.save(ultimoPago);
                }
            }
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

        long timeoutSeconds = Math.max(60, Duration.between(ahora, reservaInfo.expiraEn()).getSeconds());
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
            if (expiraEn != null && expiraEn.isAfter(LocalDateTime.now())) {
                puedeReintentar = true;
            }
        }

        MedioPagoDto medioPago = extraerMedioPago(pago);
        String motivoRechazo = extraerMotivoRechazo(pago);
        Long boletaId = pago.getEstado() == EstadoPago.PAGADO ? pago.getId() : null;

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
            throw new IllegalArgumentException("Token de Flow ausente");
        }

        Pago pago = pagoRepository.findByFlowToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe pago asociado al token de Flow")
                        .conCodigo("PAGO_NO_ENCONTRADO"));

        if (pago.getEstado() == EstadoPago.PAGADO && tieneBoleta(pago)) {
            log.info("Notificación ya procesada con éxito anteriormente para pagoId={}", pago.getId());
            return;
        }

        FlowPaymentStatusResponse flowStatus = flowClient.obtenerEstadoPago(token);

        if (flowStatus.amount() != null && flowStatus.amount() != pago.getMonto().intValue()) {
            log.warn("Monto informado por Flow ({}) difiere de pago ({})", flowStatus.amount(), pago.getMonto());
            pago.setEstado(EstadoPago.PENDIENTE);
            agregarADetalleRespuesta(pago, "sinReserva", true);
            agregarADetalleRespuesta(pago, "motivo", "MONTO_INVALIDO");

            ejecutarDevolucionFlow(pago, flowStatus.amount(), flowStatus.payer());
            pagoRepository.save(pago);
            return;
        }

        EstadoPago nuevoEstado = mapearEstadoFlow(flowStatus.status());
        pago.setEstado(nuevoEstado);

        if (flowStatus.paymentData() != null) {
            String media = flowStatus.paymentData().media();
            String brand = flowStatus.paymentData().card() != null ? flowStatus.paymentData().card().brand() : "";
            pago.setMedioPago(brand != null && !brand.isBlank() ? "Tarjeta de Crédito " + brand : (media != null ? media : "Flow"));
        }

        LocalDateTime fechaPagoTime = LocalDateTime.now();
        pago.setFechaPago(fechaPagoTime);

        guardarRespuestaRaw(pago, flowStatus);

        Long reservaId = pago.getReserva().getId();

        if (nuevoEstado == EstadoPago.PAGADO) {
            BoletaResponse boleta = emitirBoleta(pago);
            guardarBoletaEnDetalle(pago, boleta);
        }

        ResultadoPagoRequest resultadoRequest = new ResultadoPagoRequest(
                pago.getId(),
                pago.getFlowOrder() != null ? pago.getFlowOrder() : pago.getId(),
                nuevoEstado,
                pago.getMonto().intValue(),
                fechaPagoTime.toInstant(ZoneOffset.UTC),
                nuevoEstado == EstadoPago.RECHAZADO ? "Rechazado o cancelado en Flow" : null
        );

        ReservasNotificationClient.ReservasNotificacionResult res =
                reservasNotificationClient.notificarResultado(reservaId, resultadoRequest);

        if (!res.exitoso()) {
            agregarADetalleRespuesta(pago, "reservasError", res.errorCode());
            if ("ESTADO_INVALIDO".equals(res.errorCode()) ||
                "PAGO_YA_PAGADO".equals(res.errorCode()) ||
                "MONTO_INVALIDO".equals(res.errorCode())) {

                log.warn("Reservas rechazó resultado con {}. Procediendo a reembolso en Flow.", res.errorCode());
                pago.setEstado(EstadoPago.PENDIENTE);
                agregarADetalleRespuesta(pago, "sinReserva", true);
                agregarADetalleRespuesta(pago, "motivo", res.errorCode());
                ejecutarDevolucionFlow(pago, pago.getMonto().intValue(), flowStatus.payer());
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
            throw new IllegalArgumentException("Token de reembolso ausente");
        }

        var refundStatus = flowClient.obtenerEstadoReembolso(token);

        List<Pago> pagos = pagoRepository.findAll();
        Pago pagoAfectado = null;
        for (Pago p : pagos) {
            if (p.getDetalleRespuesta() != null && p.getDetalleRespuesta().contains(token)) {
                pagoAfectado = p;
                break;
            }
        }

        if (pagoAfectado == null) {
            throw new RecursoNoEncontradoException("No se encontró pago para el reembolso token: " + token)
                    .conCodigo("PAGO_NO_ENCONTRADO");
        }

        agregarADetalleRespuesta(pagoAfectado, "refundStatus", refundStatus.status());
        pagoRepository.save(pagoAfectado);
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
    public PaginaResponse<BoletaResumenResponse> obtenerHistorialBoletas(int pagina, int tamano) {
        if (tamano > 50) {
            throw new IllegalArgumentException("El tamaño máximo permitido es 50");
        }
        UsuarioAutenticado usuario = usuarioActual.exigir();
        if (usuario.rol() != RolUsuario.PASAJERO) {
            throw new AccessDeniedException("Solo pasajeros pueden consultar su historial de boletas");
        }

        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Pago> pagosPagados = pagoRepository.findPagadosByUsuarioId(usuario.id(), pageable);

        List<BoletaResumenResponse> resumenes = pagosPagados.getContent().stream()
                .map(p -> {
                    BoletaResponse b = leerBoletaDesdeDetalle(p);
                    return new BoletaResumenResponse(
                            b.boletaId(),
                            b.numeroBoleta(),
                            b.codigoReserva(),
                            b.fechaEmision(),
                            b.totalCLP()
                    );
                })
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
    public byte[] generarBoletaPdf(Long boletaId, String compraToken) {
        Pago pago = pagoRepository.findById(boletaId)
                .filter(p -> p.getEstado() == EstadoPago.PAGADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        reservaAcceso.obtenerAutorizada(pago.getReserva().getId(), compraToken);
        BoletaResponse boleta = leerBoletaDesdeDetalle(pago);
        return boletaPdfGenerator.generarBoletaPdf(boleta);
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse consultarBoletaInvitado(ConsultaBoletaInvitadoRequest request) {
        Reserva reserva = reservaRepository.findAll().stream()
                .filter(r -> r.getCodigoReserva() != null && r.getCodigoReserva().equalsIgnoreCase(request.codigoReserva()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada")
                        .conCodigo("BOLETA_NO_ENCONTRADA"));

        if (reserva.getEmailContacto() == null || !reserva.getEmailContacto().equalsIgnoreCase(request.correo())) {
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
    public ResumenPagosResponse obtenerResumenReportes(LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : LocalDate.now(ZONA_CHILE).minusMonths(1).atStartOfDay();
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : LocalDate.now(ZONA_CHILE).atTime(LocalTime.MAX);

        List<Pago> pagosEnRango = pagoRepository.findByRangoFechas(desdeDateTime, hastaDateTime);

        int cantidadPagos = 0;
        long montoTotalCLP = 0;
        int cantidadRechazados = 0;
        Map<String, List<Pago>> pagosPorMedio = new HashMap<>();

        for (Pago p : pagosEnRango) {
            if (p.getEstado() == EstadoPago.PAGADO) {
                cantidadPagos++;
                long monto = p.getMonto() != null ? p.getMonto().longValue() : 0;
                montoTotalCLP += monto;

                String medio = p.getMedioPago() != null && !p.getMedioPago().isBlank() ? p.getMedioPago() : "Otros";
                pagosPorMedio.computeIfAbsent(medio, k -> new ArrayList<>()).add(p);
            } else if (p.getEstado() == EstadoPago.RECHAZADO) {
                cantidadRechazados++;
            }
        }

        long ticketPromedio = cantidadPagos > 0 ? (montoTotalCLP / cantidadPagos) : 0;

        List<TotalMedioPagoDto> porMedioList = new ArrayList<>();
        for (Map.Entry<String, List<Pago>> entry : pagosPorMedio.entrySet()) {
            long totalMedio = entry.getValue().stream().mapToLong(p -> p.getMonto().longValue()).sum();
            porMedioList.add(new TotalMedioPagoDto(entry.getKey(), entry.getValue().size(), totalMedio));
        }

        return new ResumenPagosResponse(
                desdeDateTime.toLocalDate(),
                hastaDateTime.toLocalDate(),
                cantidadPagos,
                montoTotalCLP,
                ticketPromedio,
                cantidadRechazados,
                porMedioList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<PagoRegistroResponse> obtenerPagosReportes(LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        if (tamano > 50) {
            throw new IllegalArgumentException("El tamaño máximo de página es 50");
        }
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Pago> pagosPage = pagoRepository.findByRangoFechasPaginado(desdeDateTime, hastaDateTime, pageable);

        List<PagoRegistroResponse> registros = pagosPage.getContent().stream()
                .map(p -> new PagoRegistroResponse(
                        p.getId(),
                        p.getReserva().getId(),
                        p.getReserva().getCodigoReserva(),
                        p.getEstado(),
                        p.getMonto() != null ? p.getMonto().intValue() : 0,
                        p.getMedioPago(),
                        p.getFechaPago() != null ? p.getFechaPago().toInstant(ZoneOffset.UTC) : null,
                        p.getCreatedAt() != null ? p.getCreatedAt().toInstant(ZoneOffset.UTC) : Instant.now()
                ))
                .toList();

        return new PaginaResponse<>(
                registros,
                pagosPage.getNumber(),
                pagosPage.getSize(),
                pagosPage.getTotalElements(),
                pagosPage.getTotalPages()
        );
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
        return expiraEn == null || expiraEn.isAfter(LocalDateTime.now());
    }

    // ---------- Métodos Auxiliares ----------

    private void validarServiceKey(String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank() || !serviceKey.equals(configuredServiceKey)) {
            throw new AuthenticationCredentialsNotFoundException("Clave compartida X-Service-Key inválida o ausente");
        }
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

    private void ejecutarDevolucionFlow(Pago pago, int monto, String emailPagador) {
        try {
            String receiverEmail = emailPagador != null && !emailPagador.isBlank()
                    ? emailPagador
                    : pago.getReserva().getEmailContacto();
            flowClient.crearReembolso(
                    "REF-" + pago.getId(),
                    receiverEmail,
                    monto,
                    flowUrlReembolsoCallback,
                    pago.getFlowOrder()
            );
        } catch (Exception e) {
            log.error("Error al ejecutar devolución en Flow para pagoId {}: {}", pago.getId(), e.getMessage(), e);
        }
    }

    private BoletaResponse emitirBoleta(Pago pago) {
        ReservaParaPago reservaInfo = reservaConsultaInterna.obtenerParaPago(pago.getReserva().getId());
        EmisorDto emisor = new EmisorDto(
                "BANDUAIRLINES SpA",
                "76.543.210-8",
                "Transporte Aéreo de Pasajeros",
                "Avenida Francisco Salazar 01145, Temuco, Región de La Araucanía",
                "+56 2 2946 3300"
        );

        String rutReceptor = null;
        if (pago.getReserva().getUsuario() != null) {
            Usuario u = pago.getReserva().getUsuario();
            if (u.getTipoDocumento() == com.banduairlines.model.enums.TipoDocumento.RUT) {
                rutReceptor = u.getNumeroDocumento();
            }
        }
        ReceptorDto receptor = new ReceptorDto(reservaInfo.contacto().nombreCompleto(), rutReceptor);

        List<String> nombresPasajeros = reservaInfo.pasajeros().stream()
                .map(p -> (p.nombres() + " " + p.apellidos()).trim())
                .toList();

        List<LineaBoletaDto> lineasBoleta = reservaInfo.total().lineas().stream()
                .map(l -> new LineaBoletaDto(l.concepto(), l.descripcion(), l.montoCLP()))
                .toList();

        MedioPagoDto medioPago = extraerMedioPago(pago);
        PagoBoletaDto pagoBoleta = new PagoBoletaDto(
                EstadoPago.PAGADO,
                medioPago,
                pago.getFlowOrder(),
                Instant.now()
        );

        long numeroBoleta = 2000000L + pago.getId();

        return new BoletaResponse(
                pago.getId(),
                numeroBoleta,
                emisor,
                Instant.now(),
                "VENTA_WEB",
                pago.getReserva().getId(),
                pago.getReserva().getCodigoReserva(),
                receptor,
                nombresPasajeros,
                lineasBoleta,
                reservaInfo.total().descuentoCLP(),
                pago.getMonto().intValue(),
                pagoBoleta
        );
    }

    private void guardarBoletaEnDetalle(Pago pago, BoletaResponse boleta) {
        try {
            Map<String, Object> map = leerDetalleComoMap(pago);
            map.put("boleta", boleta);
            pago.setDetalleRespuesta(objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.error("Error al guardar boleta en detalleRespuesta: {}", e.getMessage(), e);
        }
    }

    private BoletaResponse leerBoletaDesdeDetalle(Pago pago) {
        try {
            Map<String, Object> map = leerDetalleComoMap(pago);
            if (map.containsKey("boleta")) {
                String boletaJson = objectMapper.writeValueAsString(map.get("boleta"));
                return objectMapper.readValue(boletaJson, BoletaResponse.class);
            }
        } catch (Exception e) {
            log.warn("No se pudo deserializar boleta de detalleRespuesta, reconstruyendo: {}", e.getMessage());
        }
        return emitirBoleta(pago);
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
        try {
            Map<String, Object> map = leerDetalleComoMap(pago);
            map.put("flowResponse", flowStatus);
            pago.setDetalleRespuesta(objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.error("Error al guardar respuesta cruda: {}", e.getMessage(), e);
        }
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
        if (pago.getMedioPago() != null && !pago.getMedioPago().isBlank()) {
            return new MedioPagoDto(pago.getMedioPago(), "4521");
        }
        return null;
    }

    private String extraerMotivoRechazo(Pago pago) {
        Map<String, Object> map = leerDetalleComoMap(pago);
        if (map.containsKey("motivo")) {
            return String.valueOf(map.get("motivo"));
        }
        return null;
    }
}
