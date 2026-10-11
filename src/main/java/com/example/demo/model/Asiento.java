package com.example.demo.model;

import com.example.demo.model.enums.ClaseCabina;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asientos", uniqueConstraints = {
    @UniqueConstraint(name = "asientos_aeronave_id_fila_letra_unique", columnNames = {"aeronave_id", "fila", "letra"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aeronave_id", nullable = false)
    private Avion aeronave; 

    @Column(nullable = false)
    private Integer fila; 

    @Column(nullable = false, length = 1)
    private String letra;

    @Enumerated(EnumType.STRING)
    @Column(name = "clase_cabina", nullable = false)
    private ClaseCabina claseCabina; 

    @Column(name = "es_preferente", nullable = false)
    private Boolean esPreferente; 
}