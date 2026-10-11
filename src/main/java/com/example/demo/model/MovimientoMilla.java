package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.model.enums.TipoMovimiento;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_millas", indexes = {
    @Index(name = "movimientos_millas_usuario_id_created_at_index", columnList = "usuario_id, created_at") //[cite: 2]
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoMilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private CuentaFidelizacion cuentaFidelizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer millas; 

    @Column(length = 200)
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 
}