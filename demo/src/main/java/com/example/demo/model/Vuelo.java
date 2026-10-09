package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.model.enums.EstadoVuelo;

import java.time.LocalDateTime;

@Entity
@Table(name = "vuelos", 
    uniqueConstraints = {
        @UniqueConstraint(name = "vuelos_codigo_vuelo_salida_programada_unique", columnNames = {"codigo_vuelo", "salida_programada"})
    },
    indexes = {
        @Index(name = "vuelos_ruta_id_salida_programada_index", columnList = "ruta_id, salida_programada"),
        @Index(name = "vuelos_aeronave_id_salida_programada_index", columnList = "aeronave_id, salida_programada")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    @Column(name = "codigo_vuelo", nullable = false, length = 10)
    private String codigoVuelo; 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aeronave_id", nullable = false)
    private Avion aeronave; 

    @Column(name = "salida_programada", nullable = false)
    private LocalDateTime salidaProgramada; 

    @Column(name = "llegada_programada", nullable = false)
    private LocalDateTime llegadaProgramada; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoVuelo estado = EstadoVuelo.PROGRAMADO; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 
}