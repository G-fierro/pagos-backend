package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banduairlines.model.enums.TipoDescuento;

@Entity
@Table(name = "promociones", uniqueConstraints = {
    @UniqueConstraint(name = "promociones_codigo_unique", columnNames = {"codigo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo; 

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false)
    private TipoDescuento tipoDescuento; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDateTime vigenteDesde;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDateTime vigenteHasta;

    @Column(name = "usos_maximos")
    private Integer usosMaximos; 

    @Column(name = "usos_actuales", nullable = false)
    @Builder.Default
    private Integer usosActuales = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true; 
}