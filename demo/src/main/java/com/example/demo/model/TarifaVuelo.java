package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.example.demo.model.enums.ClaseCabina;

@Entity
@Table(name = "tarifas_vuelo", uniqueConstraints = {
    @UniqueConstraint(name = "tarifas_vuelo_vuelo_id_clase_cabina_pack_id_unique", columnNames = {"vuelo_id", "clase_cabina", "pack_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo; 

    @Enumerated(EnumType.STRING)
    @Column(name = "clase_cabina", nullable = false)
    private ClaseCabina claseCabina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private PackTarifario pack;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio; 
}