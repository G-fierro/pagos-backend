package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "packs_tarifarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackTarifario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; 

    @Column(length = 255)
    private String descripcion; 

    @Column(name = "incluye_equipaje_cabina", nullable = false)
    private Boolean incluyeEquipajeCabina; 

    @Column(name = "piezas_bodega_incluidas", nullable = false)
    private Integer piezasBodegaIncluidas;

    @Column(name = "kg_por_pieza_bodega", nullable = false)
    private Integer kgPorPiezaBodega; 

    @Column(name = "incluye_seleccion_asiento", nullable = false)
    private Boolean incluyeSeleccionAsiento;

    @Column(name = "incluye_embarque_prioritario", nullable = false)
    private Boolean incluyeEmbarquePrioritario;

    @Column(name = "permite_cambios", nullable = false)
    private Boolean permiteCambios; 

    @Column(name = "multiplicador_millas", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal multiplicadorMillas = BigDecimal.ONE; 

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true; 
}