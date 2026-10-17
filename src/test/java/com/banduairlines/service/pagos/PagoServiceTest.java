package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.*;
import com.banduairlines.exception.ConflictoException;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ServicioExternoException;
import jakarta.validation.ConstraintViolationException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaConsultaInterna reservaConsultaInterna;
    @Mock
    private ReservaAcceso reservaAcceso;
    @Mock
    private FlowClient flowClient;
    @Mock
    private ReservasNotificationClient reservasNotificationClient;
    @Mock
    private UsuarioActual usuarioActual;

    private BoletaPdfGenerator boletaPdfGenerator;
    private ObjectMapper objectMapper;
    private PagoServiceImpl pagoService;

    private final String serviceKey = "clave_compartida_reservas";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        boletaPdfGenerator = new BoletaPdfGenerator();
        pagoService = new PagoServiceImpl(
                pagoRepository,
                reservaRepository,
                reservaConsultaInterna,
                reservaAcceso,
                flowClient,
                reservasNotificationClient,
                boletaPdfGenerator,
                usuarioActual
        );
        ReflectionTestUtils.setField(pagoService, "configuredServiceKey", serviceKey);
        ReflectionTestUtils.setField(pagoService, "flowUrlConfirmacion", "http://localhost:8080/api/v1/pagos/notificaciones");
        ReflectionTestUtils.setField(pagoService, "flowUrlRetorno", "http://localhost:8080/api/v1/pagos/retorno");
        ReflectionTestUtils.setField(pagoService, "flowUrlReembolsoCallback", "http://localhost:8080/api/v1/pagos/reembolsos/notificaciones");
        ReflectionTestUtils.setField(pagoService, "flowApiUrl", "https://sandbox.flow.cl/api");
    }

    @Test
    void iniciarPago_conClaveInvalida_lanza401() {
        IniciarPagoRequest req = new IniciarPagoRequest(1L);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                pagoService.iniciarPago(req, "clave_incorrecta"));
    }

    @Test
    void iniciarPago_cuandoReservaNoEstaEnPendientePago_lanza409EstadoInvalido() {
        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.CONFIRMADA, Instant.now().plusSeconds(600),
                new ContactoDto("Juan", "juan@test.com", "1234"),
                List.of(), new TotalReservaDto(List.of(), 0, 100000)
        );
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);

        ConflictoException ex = assertThrows(ConflictoException.class, () ->
                pagoService.iniciarPago(new IniciarPagoRequest(1L), serviceKey));
        assertEquals("ESTADO_INVALIDO", ex.getCodigo());
    }

    @Test
    void iniciarPago_cuandoYaTienePagoAprobado_lanza409PagoYaPagado() {
        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.PENDIENTE_PAGO, Instant.now().plusSeconds(600),
                new ContactoDto("Juan", "juan@test.com", "1234"),
                List.of(), new TotalReservaDto(List.of(), 0, 100000)
        );
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);
        when(pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(1L, EstadoPago.PAGADO))
                .thenReturn(Optional.of(new Pago()));

        ConflictoException ex = assertThrows(ConflictoException.class, () ->
                pagoService.iniciarPago(new IniciarPagoRequest(1L), serviceKey));
        assertEquals("PAGO_YA_PAGADO", ex.getCodigo());
    }

    @Test
    void iniciarPago_cuandoMontoEsCeroOMenor_lanza422MontoInvalido() {
        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.PENDIENTE_PAGO, Instant.now().plusSeconds(600),
                new ContactoDto("Juan", "juan@test.com", "1234"),
                List.of(), new TotalReservaDto(List.of(), 0, 0)
        );
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);
        when(pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(1L, EstadoPago.PAGADO))
                .thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                pagoService.iniciarPago(new IniciarPagoRequest(1L), serviceKey));
        assertEquals("MONTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void iniciarPago_exitoso_creaOrdenEnFlowYRetorna201() {
        Instant expira = Instant.now().plusSeconds(1800);
        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.PENDIENTE_PAGO, expira,
                new ContactoDto("Javiera Muñoz", "javiera@test.com", "+5691234"),
                List.of(new PasajeroCompraDto(10L, "Javiera", "Muñoz")),
                new TotalReservaDto(List.of(new LineaCompraDto("TARIFA", "Tarifa", 255320)), 0, 255320)
        );
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").total(BigDecimal.valueOf(255320)).build();

        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);
        when(pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(1L, EstadoPago.PAGADO)).thenReturn(Optional.empty());
        when(pagoRepository.findByReservaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        Pago pagoGuardado = Pago.builder().id(100L).reserva(reserva).monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoGuardado);

        FlowPaymentCreateResponse flowResp = new FlowPaymentCreateResponse("https://sandbox.flow.cl/app/web/pay.php", "FLOW_TOKEN_123", 98765L);
        when(flowClient.crearPago(anyString(), anyString(), anyInt(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(flowResp);

        PagoIniciadoResponse response = pagoService.iniciarPago(new IniciarPagoRequest(1L), serviceKey);

        assertNotNull(response);
        assertEquals(100L, response.pagoId());
        assertEquals("K7QX2M", response.codigoReserva());
        assertEquals(EstadoPago.PENDIENTE, response.estado());
        assertTrue(response.urlPago().contains("FLOW_TOKEN_123"));
        assertEquals(255320, response.montoCLP());
    }

    @Test
    void obtenerPago_cuandoNoExiste_lanza404() {
        when(pagoRepository.findById(999L)).thenReturn(Optional.empty());
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () ->
                pagoService.obtenerPago(999L, null));
        assertEquals("PAGO_NO_ENCONTRADO", ex.getCodigo());
    }

    @Test
    void procesarNotificacionFlow_cuandoMontoCoincideYEsPagado_emiteBoletaYNotificaReservas() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_ABC").monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();

        when(pagoRepository.findByFlowToken("TOKEN_ABC")).thenReturn(Optional.of(pago));

        FlowPaymentStatusResponse flowStatus = new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 2, 255320, "CLP", "cliente@test.com",
                new FlowPaymentStatusResponse.PaymentData("2026-09-27", "webpay", new FlowPaymentStatusResponse.CardData("credit", "Visa", "4521"))
        );
        when(flowClient.obtenerEstadoPago("TOKEN_ABC")).thenReturn(flowStatus);

        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.PENDIENTE_PAGO, Instant.now().plusSeconds(600),
                new ContactoDto("Javiera", "javiera@test.com", "1234"),
                List.of(), new TotalReservaDto(List.of(new LineaCompraDto("TARIFA", "Vuelo", 255320)), 0, 255320)
        );
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);

        when(reservasNotificationClient.notificarResultado(eq(1L), any(ResultadoPagoRequest.class)))
                .thenReturn(new ReservasNotificationClient.ReservasNotificacionResult(true, 200, null, "OK"));

        pagoService.procesarNotificacionFlow("TOKEN_ABC");

        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals("Tarjeta de Crédito Visa", pago.getMedioPago());
        verify(reservasNotificationClient).notificarResultado(eq(1L), any(ResultadoPagoRequest.class));
        verify(pagoRepository, atLeastOnce()).save(pago);
    }

    @Test
    void procesarNotificacionFlow_cuandoMontoDifiere_dejaPendienteYDevuelvePorFlow() {
        Reserva reserva = Reserva.builder().id(1L).emailContacto("cliente@test.com").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_DIFF").monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();

        when(pagoRepository.findByFlowToken("TOKEN_DIFF")).thenReturn(Optional.of(pago));

        // Monto en Flow difiere (200000 vs 255320)
        FlowPaymentStatusResponse flowStatus = new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 2, 200000, "CLP", "cliente@test.com", null
        );
        when(flowClient.obtenerEstadoPago("TOKEN_DIFF")).thenReturn(flowStatus);

        pagoService.procesarNotificacionFlow("TOKEN_DIFF");

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        verify(flowClient).crearReembolso(eq("REF-10"), eq("cliente@test.com"), eq(200000), anyString(), any());
    }

    @Test
    void generarBoletaPdf_generaDocumentoPdfValido() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).estado(EstadoPago.PAGADO).monto(BigDecimal.valueOf(255320)).build();
        pago.setDetalleRespuesta(detalleConBoleta(10L, 255320));

        when(pagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        when(reservaAcceso.obtenerAutorizada(eq(1L), any())).thenReturn(reserva);

        BoletaPdfDto pdf = pagoService.generarBoletaPdf(10L, "TOKEN-INVITADO");

        assertEquals(2000010L, pdf.numeroBoleta());
        assertNotNull(pdf.contenido());
        assertTrue(pdf.contenido().length > 100);
        String header = new String(pdf.contenido(), 0, 8);
        assertTrue(header.startsWith("%PDF-1.4"), "El PDF debe iniciar con la cabecera estándar %PDF-1.4");
        String texto = new String(pdf.contenido(), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(texto.contains("Javiera Mu"), "El PDF muestra el receptor");
        assertFalse(texto.contains("RUT"), "La boleta no lleva RUT del emisor ni del receptor");
        verify(reservaAcceso).obtenerAutorizada(1L, "TOKEN-INVITADO");
        verifyNoInteractions(usuarioActual);
    }

    @Test
    void generarBoletaPdf_sinBoletaGuardada_lanza404SinReconstruir() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).estado(EstadoPago.PAGADO).monto(BigDecimal.valueOf(255320)).build();
        when(pagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        when(reservaAcceso.obtenerAutorizada(eq(1L), any())).thenReturn(reserva);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.generarBoletaPdf(10L, null));
        assertEquals("BOLETA_NO_ENCONTRADA", ex.getCodigo());
        verifyNoInteractions(reservaConsultaInterna);
    }

    @Test
    void procesarNotificacionFlow_sinToken_lanzaValidacion() {
        assertThrows(ConstraintViolationException.class, () -> pagoService.procesarNotificacionFlow(" "));
        assertThrows(ConstraintViolationException.class, () -> pagoService.procesarNotificacionFlow(null));
    }

    @Test
    void procesarNotificacionFlow_pagado_guardaBoletaInmutableConDatosDeFlow() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_OK").monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_OK")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_OK")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 2, 255320, "CLP", "cliente@test.com",
                new FlowPaymentStatusResponse.PaymentData("2026-09-26 20:52:00", "webpay",
                        new FlowPaymentStatusResponse.CardData("credit", "Visa", "9876"))));
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaPagada());
        when(reservasNotificationClient.notificarResultado(eq(1L), any(ResultadoPagoRequest.class)))
                .thenReturn(new ReservasNotificationClient.ReservasNotificacionResult(true, 200, null, "OK"));

        pagoService.procesarNotificacionFlow("TOKEN_OK");

        when(reservaAcceso.obtenerAutorizada(eq(1L), any())).thenReturn(reserva);
        when(pagoRepository.findFirstByReservaIdAndEstadoOrderByIdDesc(1L, EstadoPago.PAGADO)).thenReturn(Optional.of(pago));
        BoletaResponse boleta = pagoService.obtenerBoletaPorReserva(1L, null);

        assertEquals(10L, boleta.boletaId());
        assertEquals(2000010L, boleta.numeroBoleta());
        assertEquals("9876", boleta.pago().medioPago().ultimosDigitos());
        assertEquals("Tarjeta de Crédito Visa", boleta.pago().medioPago().nombre());
        assertEquals(5555L, boleta.pago().ordenFlow());
        assertEquals("Javiera Fernanda Muñoz Soto", boleta.receptor());
        // 20:52 en Chile (UTC-3 en septiembre) son las 23:52 UTC, y esa es también la fecha de emisión.
        assertEquals(Instant.parse("2026-09-26T23:52:00Z"), boleta.pago().fechaPago());
        assertEquals(boleta.pago().fechaPago(), boleta.fechaEmision());
        assertEquals(LocalDateTime.of(2026, 9, 26, 23, 52), pago.getFechaPago());
        assertEquals(255320, boleta.totalCLP());
    }

    @Test
    void procesarNotificacionFlow_cuandoFlowSigueEnEspera_noNotificaAReservas() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_P").monto(BigDecimal.valueOf(1000)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_P")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_P")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 1, 1000, "CLP", "cliente@test.com", null));

        pagoService.procesarNotificacionFlow("TOKEN_P");

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNull(pago.getFechaPago());
        verifyNoInteractions(reservasNotificationClient);
    }

    @Test
    void procesarNotificacionFlow_rechazado_notificaUnaSolaVezAunqueFlowRepita() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_R").monto(BigDecimal.valueOf(1000)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_R")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_R")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 3, 1000, "CLP", "cliente@test.com", null));
        when(reservasNotificationClient.notificarResultado(eq(1L), any(ResultadoPagoRequest.class)))
                .thenReturn(new ReservasNotificationClient.ReservasNotificacionResult(true, 200, null, "OK"));

        pagoService.procesarNotificacionFlow("TOKEN_R");
        pagoService.procesarNotificacionFlow("TOKEN_R");

        assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
        // fechaPago es el momento en que Flow confirmó el pago: un rechazo no la tiene.
        assertNull(pago.getFechaPago());
        verify(reservasNotificationClient, times(1)).notificarResultado(eq(1L), any(ResultadoPagoRequest.class));
    }

    @Test
    void procesarNotificacionFlow_cuandoReservasFalla5xx_noModificaElPago() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_5XX").monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_5XX")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_5XX")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 2, 255320, "CLP", "cliente@test.com", null));
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaPagada());
        when(reservasNotificationClient.notificarResultado(eq(1L), any(ResultadoPagoRequest.class)))
                .thenThrow(new ServicioExternoException("Reservas caído", new RuntimeException()));

        assertThrows(ServicioExternoException.class, () -> pagoService.procesarNotificacionFlow("TOKEN_5XX"));

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNull(pago.getDetalleRespuesta());
        assertNull(pago.getFechaPago());
    }

    @Test
    void procesarNotificacionFlow_cuandoReservaCancelada_devuelveCobroUnaSolaVezYNoEmiteBoleta() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").emailContacto("contacto@test.com").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_CANC").flowOrder(5555L).monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_CANC")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_CANC")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 2, 255320, "CLP", "pagador@test.com",
                new FlowPaymentStatusResponse.PaymentData("2026-09-26 20:52:00", "webpay",
                        new FlowPaymentStatusResponse.CardData("credit", "Visa", "4521"))));
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaPagada());
        when(reservasNotificationClient.notificarResultado(eq(1L), any(ResultadoPagoRequest.class)))
                .thenReturn(new ReservasNotificationClient.ReservasNotificacionResult(false, 409, "ESTADO_INVALIDO", "cancelada"));
        when(flowClient.crearReembolso(eq("REF-10"), eq("pagador@test.com"), eq(255320), anyString(), eq(5555L)))
                .thenReturn(new FlowRefundResponse("refund", 777L, "2026-09-27", "created", java.math.BigDecimal.valueOf(255320), "REF_TOKEN_1"));

        pagoService.procesarNotificacionFlow("TOKEN_CANC");
        // Flow vuelve a avisar: no debe reprocesar ni pedir otro reembolso.
        pagoService.procesarNotificacionFlow("TOKEN_CANC");

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNull(pago.getFechaPago());
        // Un pago devuelto por Flow sigue PENDIENTE y no expone medio de pago.
        assertNull(pago.getMedioPago());
        when(pagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        assertNull(pagoService.obtenerPago(10L, null).medioPago());
        assertTrue(pago.getDetalleRespuesta().contains("\"sinReserva\":true"));
        assertTrue(pago.getDetalleRespuesta().contains("\"motivo\":\"ESTADO_INVALIDO\""));
        assertTrue(pago.getDetalleRespuesta().contains("\"token\":\"REF_TOKEN_1\""));
        assertTrue(pago.getDetalleRespuesta().contains("\"flowRefundOrder\":777"));
        assertFalse(pago.getDetalleRespuesta().contains("\"boleta\""));
        verify(flowClient, times(1)).crearReembolso(anyString(), anyString(), anyInt(), anyString(), anyLong());
        verify(reservasNotificationClient, times(1)).notificarResultado(eq(1L), any(ResultadoPagoRequest.class));
    }

    @Test
    void procesarNotificacionFlow_cuandoMontoDifierePeroNoEstaPagado_noDevuelve() {
        Reserva reserva = Reserva.builder().id(1L).build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).flowToken("TOKEN_X").monto(BigDecimal.valueOf(255320)).estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findByFlowToken("TOKEN_X")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoPago("TOKEN_X")).thenReturn(new FlowPaymentStatusResponse(
                5555L, "PAGO-10", 1, 200000, "CLP", "cliente@test.com", null));

        pagoService.procesarNotificacionFlow("TOKEN_X");

        verify(flowClient, never()).crearReembolso(any(), any(), any(), any(), any());
    }

    @Test
    void procesarNotificacionReembolso_actualizaEstadoDelReembolso() {
        Reserva reserva = Reserva.builder().id(1L).build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).estado(EstadoPago.PENDIENTE).build();
        pago.setDetalleRespuesta("{\"sinReserva\":true,\"reembolso\":{\"token\":\"REF_TOKEN_1\",\"estado\":\"created\"}}");
        when(pagoRepository.findByReembolsoToken("REF_TOKEN_1")).thenReturn(Optional.of(pago));
        when(flowClient.obtenerEstadoReembolso("REF_TOKEN_1"))
                .thenReturn(new FlowRefundResponse("refund", 777L, "2026-09-27", "accepted", java.math.BigDecimal.valueOf(255320), "REF_TOKEN_1"));

        pagoService.procesarNotificacionReembolso("REF_TOKEN_1");

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertTrue(pago.getDetalleRespuesta().contains("\"estado\":\"accepted\""));
        assertTrue(pago.getDetalleRespuesta().contains("\"token\":\"REF_TOKEN_1\""));
        assertTrue(pago.getDetalleRespuesta().contains("\"sinReserva\":true"));
        verify(pagoRepository).save(pago);
    }

    @Test
    void procesarNotificacionReembolso_validaToken() {
        assertThrows(ConstraintViolationException.class, () -> pagoService.procesarNotificacionReembolso(null));

        when(pagoRepository.findByReembolsoToken("DESCONOCIDO")).thenReturn(Optional.empty());
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.procesarNotificacionReembolso("DESCONOCIDO"));
        assertEquals("PAGO_NO_ENCONTRADO", ex.getCodigo());
    }

    private ReservaParaPago reservaPagada() {
        return new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.PENDIENTE_PAGO, Instant.now().plusSeconds(600),
                new ContactoDto("Javiera Fernanda Muñoz Soto", "javiera@test.com", "1234"),
                List.of(new PasajeroCompraDto(1L, "Javiera Fernanda", "Muñoz Soto")),
                new TotalReservaDto(List.of(new LineaCompraDto("TARIFA", "Tarifa Light × 1 adulto", 255320)), 0, 255320)
        );
    }

    private String detalleConBoleta(Long pagoId, int total) {
        try {
            BoletaResponse boleta = new BoletaResponse(
                    pagoId, 2000000L + pagoId,
                    Instant.parse("2026-09-27T00:52:00Z"), "VENTA_WEB", 1L, "K7QX2M",
                    "Javiera Muñoz", List.of("Javiera Muñoz"),
                    List.of(new LineaBoletaDto("TARIFA", "Tarifa Light × 1 adulto", total)), 0, total,
                    new PagoBoletaDto(EstadoPago.PAGADO, new MedioPagoDto("Tarjeta de Crédito Visa", "4521"), 3849571L,
                            Instant.parse("2026-09-27T00:52:00Z")));
            return objectMapper.writeValueAsString(java.util.Map.of("boleta", boleta));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    void procesarRetornoFlow_conFrontendUrlConfigurada_redirigeAlFrontendAbsoluto() {
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "https://dev.isoft.lazycat.app");
        Pago pago = Pago.builder().id(99L).flowToken("token_test_123").build();
        when(pagoRepository.findByFlowToken("token_test_123")).thenReturn(Optional.of(pago));

        String redirect = pagoService.procesarRetornoFlow("token_test_123");

        assertEquals("https://dev.isoft.lazycat.app/pago/resultado?pagoId=99", redirect);
    }

    @Test
    void procesarRetornoFlow_sinFrontendUrl_redirigeConRutaRelativa() {
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "");
        Pago pago = Pago.builder().id(99L).flowToken("token_test_123").build();
        when(pagoRepository.findByFlowToken("token_test_123")).thenReturn(Optional.of(pago));

        String redirect = pagoService.procesarRetornoFlow("token_test_123");

        assertEquals("/pago/resultado?pagoId=99", redirect);
    }
}
