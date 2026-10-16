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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                usuarioActual,
                objectMapper
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
    void consultarBoletaInvitado_cuandoCodigoOCorreoNoCoinciden_lanza404() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").emailContacto("correcto@test.com").build();
        when(reservaRepository.findAll()).thenReturn(List.of(reserva));

        ConsultaBoletaInvitadoRequest req = new ConsultaBoletaInvitadoRequest("K7QX2M", "incorrecto@test.com");
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () ->
                pagoService.consultarBoletaInvitado(req));
        assertEquals("BOLETA_NO_ENCONTRADA", ex.getCodigo());
    }

    @Test
    void generarBoletaPdf_generaDocumentoPdfValido() {
        Reserva reserva = Reserva.builder().id(1L).codigoReserva("K7QX2M").build();
        Pago pago = Pago.builder().id(10L).reserva(reserva).estado(EstadoPago.PAGADO).monto(BigDecimal.valueOf(255320)).build();

        when(pagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        when(reservaAcceso.obtenerAutorizada(eq(1L), any())).thenReturn(reserva);

        ReservaParaPago reservaInfo = new ReservaParaPago(
                1L, "K7QX2M", EstadoReserva.CONFIRMADA, Instant.now(),
                new ContactoDto("Javiera", "javiera@test.com", "1234"),
                List.of(new PasajeroCompraDto(1L, "Javiera", "Muñoz")),
                new TotalReservaDto(List.of(new LineaCompraDto("TARIFA", "Vuelo", 255320)), 0, 255320)
        );
        when(reservaConsultaInterna.obtenerParaPago(1L)).thenReturn(reservaInfo);

        byte[] pdf = pagoService.generarBoletaPdf(10L, null);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        String header = new String(pdf, 0, 8);
        assertTrue(header.startsWith("%PDF-1.4"), "El PDF debe iniciar con la cabecera estándar %PDF-1.4");
    }

    @Test
    void obtenerResumenReportes_calculaTotalesYTicketPromedioCorrectamente() {
        Pago p1 = Pago.builder().id(1L).estado(EstadoPago.PAGADO).monto(BigDecimal.valueOf(100000)).medioPago("Tarjeta de Crédito Visa").fechaPago(LocalDateTime.now()).build();
        Pago p2 = Pago.builder().id(2L).estado(EstadoPago.PAGADO).monto(BigDecimal.valueOf(200000)).medioPago("Tarjeta de Crédito Visa").fechaPago(LocalDateTime.now()).build();
        Pago p3 = Pago.builder().id(3L).estado(EstadoPago.RECHAZADO).monto(BigDecimal.valueOf(50000)).fechaPago(LocalDateTime.now()).build();

        when(pagoRepository.findByRangoFechas(any(), any())).thenReturn(List.of(p1, p2, p3));

        ResumenPagosResponse resumen = pagoService.obtenerResumenReportes(LocalDate.now().minusDays(5), LocalDate.now());

        assertNotNull(resumen);
        assertEquals(2, resumen.cantidadPagos());
        assertEquals(300000L, resumen.montoTotalCLP());
        assertEquals(150000L, resumen.ticketPromedioCLP());
        assertEquals(1, resumen.cantidadRechazados());
        assertEquals(1, resumen.porMedioPago().size());
        assertEquals("Tarjeta de Crédito Visa", resumen.porMedioPago().get(0).medioPago());
        assertEquals(2, resumen.porMedioPago().get(0).cantidadPagos());
        assertEquals(300000L, resumen.porMedioPago().get(0).montoTotalCLP());
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
