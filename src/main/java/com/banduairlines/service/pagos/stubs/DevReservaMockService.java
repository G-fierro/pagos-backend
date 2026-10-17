package com.banduairlines.service.pagos.stubs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.model.Pago;
import com.banduairlines.model.Pasajero;
import com.banduairlines.model.Reserva;
import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;
import com.banduairlines.repository.PagoRepository;
import com.banduairlines.repository.PasajeroRepository;
import com.banduairlines.repository.ReservaRepository;
import com.banduairlines.repository.UsuarioRepository;

/**
 * Servicio de stubs para el módulo de Reservas en entornos de desarrollo y pruebas.
 * Garantiza que existan reservas con vigencia fresca, pasajeros y datos consistentes
 * para que el flujo de cobro con Flow, notificaciones internas y consultas de boleta
 * funcionen de manera predecible.
 */
@Service
public class DevReservaMockService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevReservaMockService.class);

    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    public DevReservaMockService(
            ReservaRepository reservaRepository,
            PasajeroRepository pasajeroRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            asegurarDatosPruebaIniciales();
        } catch (Exception e) {
            log.warn("No se pudieron inicializar reservas dev en arranque: {}", e.getMessage());
        }
    }

    @Transactional
    public void asegurarDatosPruebaIniciales() {
        Optional<Reserva> res1Opt = reservaRepository.findById(1L);
        if (res1Opt.isEmpty()) {
            log.info("Sembrando datos de reserva inicial 1 (K7QX2M) para desarrollo...");
            sembrarReservaPrincipal();
        } else {
            Reserva res1 = res1Opt.get();
            // Si la reserva 1 ya venció, renovamos su vigencia para que las pruebas funcionen de inmediato
            if (res1.getExpiraEn() == null || res1.getExpiraEn().isBefore(LocalDateTime.now())) {
                log.info("Renovando vigencia de reserva 1 (K7QX2M) para pruebas de desarrollo...");
                res1.setExpiraEn(LocalDateTime.now().plusDays(2));
                if (res1.getEstado() == EstadoReserva.CANCELADA) {
                    res1.setEstado(EstadoReserva.PENDIENTE_PAGO);
                }
                reservaRepository.save(res1);
            }
        }
    }

    @Transactional
    public Reserva sembrarReservaPrincipal() {
        Usuario usuario = asegurarUsuarioPrueba("javiera.munoz@gmail.com", "Javiera Fernanda", "Muñoz Soto", RolUsuario.PASAJERO);

        Optional<Reserva> existente = reservaRepository.findByCodigoReservaIgnoreCase("K7QX2M");
        Reserva reserva;
        if (existente.isPresent()) {
            reserva = existente.get();
            reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
            reserva.setExpiraEn(LocalDateTime.now().plusDays(2));
            reserva.setSubtotal(BigDecimal.valueOf(300000));
            reserva.setDescuento(BigDecimal.valueOf(44680));
            reserva.setTotal(BigDecimal.valueOf(255320));
            reserva.setEmailContacto("javiera.munoz@gmail.com");
            reserva.setTelefonoContacto("+56912345678");
            reserva.setUsuario(usuario);
            reserva = reservaRepository.save(reserva);
        } else {
            reserva = Reserva.builder()
                    .codigoReserva("K7QX2M")
                    .usuario(usuario)
                    .emailContacto("javiera.munoz@gmail.com")
                    .telefonoContacto("+56912345678")
                    .estado(EstadoReserva.PENDIENTE_PAGO)
                    .subtotal(BigDecimal.valueOf(300000))
                    .descuento(BigDecimal.valueOf(44680))
                    .total(BigDecimal.valueOf(255320))
                    .moneda("CLP")
                    .expiraEn(LocalDateTime.now().plusDays(2))
                    .build();
            reserva = reservaRepository.save(reserva);
        }

        List<Pasajero> pasajeros = pasajeroRepository.findByReservaIdOrderByIdAsc(reserva.getId());
        if (pasajeros.isEmpty()) {
            Pasajero p = Pasajero.builder()
                    .reserva(reserva)
                    .nombre("Javiera Fernanda")
                    .apellido("Muñoz Soto")
                    .tipo(TipoPasajero.ADULTO)
                    .tipoDocumento(TipoDocumento.RUT)
                    .numeroDocumento("18.245.673-4")
                    .fechaNacimiento(LocalDate.of(1995, 5, 15))
                    .build();
            pasajeroRepository.save(p);
        }

        return reserva;
    }

    @Transactional
    public void sembrarTodasLasReservas() {
        sembrarReservaPrincipal();

        // 2. Reserva Vencida (para probar error 409 por expiración)
        if (reservaRepository.findByCodigoReservaIgnoreCase("VENC99").isEmpty()) {
            Usuario u = asegurarUsuarioPrueba("vencido@banduairlines.com", "Carlos", "Perez", RolUsuario.PASAJERO);
            Reserva vencida = Reserva.builder()
                    .codigoReserva("VENC99")
                    .usuario(u)
                    .emailContacto("vencido@banduairlines.com")
                    .telefonoContacto("+56999999999")
                    .estado(EstadoReserva.PENDIENTE_PAGO)
                    .subtotal(BigDecimal.valueOf(100000))
                    .descuento(BigDecimal.ZERO)
                    .total(BigDecimal.valueOf(100000))
                    .moneda("CLP")
                    .expiraEn(LocalDateTime.now().minusDays(1)) // Ya expirada
                    .build();
            vencida = reservaRepository.save(vencida);
            pasajeroRepository.save(Pasajero.builder()
                    .reserva(vencida)
                    .nombre("Carlos")
                    .apellido("Perez")
                    .tipo(TipoPasajero.ADULTO)
                    .tipoDocumento(TipoDocumento.RUT)
                    .numeroDocumento("15.111.222-3")
                    .fechaNacimiento(LocalDate.of(1990, 1, 1))
                    .build());
        }

        // 3. Reserva Confirmada / Pagada (para probar 409 PAGO_YA_PAGADO)
        if (reservaRepository.findByCodigoReservaIgnoreCase("CONF88").isEmpty()) {
            Usuario u = asegurarUsuarioPrueba("confirmado@banduairlines.com", "Ana", "Rojas", RolUsuario.PASAJERO);
            Reserva confirmada = Reserva.builder()
                    .codigoReserva("CONF88")
                    .usuario(u)
                    .emailContacto("confirmado@banduairlines.com")
                    .telefonoContacto("+56988888888")
                    .estado(EstadoReserva.CONFIRMADA)
                    .subtotal(BigDecimal.valueOf(150000))
                    .descuento(BigDecimal.ZERO)
                    .total(BigDecimal.valueOf(150000))
                    .moneda("CLP")
                    .expiraEn(LocalDateTime.now().plusDays(5))
                    .build();
            confirmada = reservaRepository.save(confirmada);
            pasajeroRepository.save(Pasajero.builder()
                    .reserva(confirmada)
                    .nombre("Ana")
                    .apellido("Rojas")
                    .tipo(TipoPasajero.ADULTO)
                    .tipoDocumento(TipoDocumento.PASAPORTE)
                    .numeroDocumento("PAS-123456")
                    .fechaNacimiento(LocalDate.of(1992, 8, 20))
                    .build());
        }

        // 4. Reserva Invitado (sin usuario, para probar X-Compra-Token)
        if (reservaRepository.findByCodigoReservaIgnoreCase("GUEST7").isEmpty()) {
            Reserva invitado = Reserva.builder()
                    .codigoReserva("GUEST7")
                    .usuario(null)
                    .emailContacto("invitado@banduairlines.com")
                    .telefonoContacto("+56977777777")
                    .estado(EstadoReserva.PENDIENTE_PAGO)
                    .subtotal(BigDecimal.valueOf(80000))
                    .descuento(BigDecimal.ZERO)
                    .total(BigDecimal.valueOf(80000))
                    .moneda("CLP")
                    .expiraEn(LocalDateTime.now().plusDays(2))
                    .build();
            invitado = reservaRepository.save(invitado);
            pasajeroRepository.save(Pasajero.builder()
                    .reserva(invitado)
                    .nombre("Pedro")
                    .apellido("Gómez")
                    .tipo(TipoPasajero.ADULTO)
                    .tipoDocumento(TipoDocumento.RUT)
                    .numeroDocumento("17.444.555-6")
                    .fechaNacimiento(LocalDate.of(1988, 3, 10))
                    .build());
        }
    }

    @Transactional
    public Reserva restablecerReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));

        // Limpiar pagos previos para poder reiniciar el cobro limpiamente
        List<Pago> pagosPrevios = pagoRepository.findByReservaIdOrderByCreatedAtDesc(reservaId);
        if (!pagosPrevios.isEmpty()) {
            pagoRepository.deleteAll(pagosPrevios);
            log.info("Eliminados {} pagos previos asociados a reservaId={}", pagosPrevios.size(), reservaId);
        }

        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        reserva.setExpiraEn(LocalDateTime.now().plusDays(2));
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva confirmarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva cancelarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    private Usuario asegurarUsuarioPrueba(String email, String nombre, String apellido, RolUsuario rol) {
        return usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = Usuario.builder()
                    .email(email)
                    .passwordHash("$2a$10$devdummyhashforlocaltestingonly12345678901234567890")
                    .nombre(nombre)
                    .apellido(apellido)
                    .rol(rol)
                    .tipoDocumento(TipoDocumento.RUT)
                    .numeroDocumento("18.245.673-4")
                    .fechaNacimiento(LocalDate.of(1995, 5, 15))
                    .telefono("+56912345678")
                    .build();
            return usuarioRepository.save(nuevo);
        });
    }
}
