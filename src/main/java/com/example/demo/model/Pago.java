package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.model.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos", 
    uniqueConstraints = {
        @UniqueConstraint(name = "pagos_flow_token_unique", columnNames = {"flow_token"}),
        @UniqueConstraint(name = "pagos_flow_order_unique", columnNames = {"flow_order"}) 
    },
    indexes = {
        @Index(name = "pagos_reserva_id_index", columnList = "reserva_id") 
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto; 

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moneda = "CLP"; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE; 

    @Column(name = "flow_token", length = 255)
    private String flowToken; 

    @Column(name = "flow_order")
    private Long flowOrder;

    @Column(name = "medio_pago", length = 50)
    private String medioPago; 

    @Column(name = "detalle_respuesta", columnDefinition = "json")
    private String detalleRespuesta; 

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; 
}