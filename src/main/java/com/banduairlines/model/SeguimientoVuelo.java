package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.banduairlines.model.enums.EstadoVuelo;

import java.time.LocalDateTime;

@Entity
@Table(name = "seguimiento_vuelo", indexes = {
    @Index(name = "seguimiento_vuelo_vuelo_id_registrado_en_index", columnList = "vuelo_id, registrado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVuelo estado;

    @Column(name = "retraso_minutos")
    private Integer retrasoMinutos;

    @Column(length = 255)
    private String observacion; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor; 

    @CreationTimestamp
    @Column(name = "registrado_en", nullable = false, updatable = false)
    private LocalDateTime registradoEn; 
}