package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.model.enums.Nivel;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas_fidelizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaFidelizacion {

    @Id
    private Long usuarioId; 

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId 
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; 

    @Column(name = "saldo_millas", nullable = false)
    private Integer saldoMillas; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Nivel nivel = Nivel.BASICO; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 
}