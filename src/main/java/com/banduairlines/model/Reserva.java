package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.banduairlines.model.enums.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Se asigna al iniciar el pago (ReservaPagoServiceImpl); mientras la compra está EN_CURSO es null.
    @Column(name = "codigo_reserva", unique = true, length = 6)
    private String codigoReserva; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; 

    @Column(name = "email_contacto", nullable = false, length = 150)
    private String emailContacto; 

    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "promocion_id")
    private Promocion promocion; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total; 

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moneda = "CLP";

    @Column(name = "expira_en")
    private LocalDateTime expiraEn; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; 
}