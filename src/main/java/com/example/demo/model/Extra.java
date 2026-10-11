package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.example.demo.model.enums.TipoExtra;

@Entity
@Table(name = "extras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Extra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoExtra tipo; 

    @Column(nullable = false, length = 80)
    private String nombre; 

    @Column(name = "peso_kg")
    private Integer pesoKg; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true; 
}