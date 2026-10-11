package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aeropuertos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aeropuerto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(name = "codigo_iata", nullable = false, unique = true, length = 3)
    private String codigoIata;

    @Column(nullable = false, length = 120)
    private String nombre; 

    @Column(nullable = false, length = 80)
    private String ciudad; 

    @Column(nullable = false, length = 60)
    @Builder.Default
    private String pais = "Chile"; 
    @Column(name = "zona_horaria", nullable = false, length = 40)
    @Builder.Default
    private String zonaHoraria = "America/Santiago"; 
}