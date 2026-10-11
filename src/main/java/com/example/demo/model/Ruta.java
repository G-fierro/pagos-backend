package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rutas", uniqueConstraints = {

    @UniqueConstraint(name = "rutas_origen_id_destino_id_unique", columnNames = {"origen_id", "destino_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = false)
    private Aeropuerto origen; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private Aeropuerto destino; 

    @Column(name = "distancia_km")
    private Integer distanciaKm;

    @Column(name = "duracion_min")
    private Integer duracionMin; 

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}