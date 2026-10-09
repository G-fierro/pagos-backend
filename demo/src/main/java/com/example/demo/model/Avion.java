package com.example.demo.model;

import com.example.demo.model.enums.Estado;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aeronaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false, unique = true, length = 15)
    private String matricula; 

    @Column(nullable = false, length = 60)
    private String modelo; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Estado estado = Estado.ACTIVA; 
}