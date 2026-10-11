package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "boleto_extras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletoExtra {

    @EmbeddedId
    @Builder.Default
    private BoletoExtraId id = new BoletoExtraId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("boletoId") 
    @JoinColumn(name = "boleto_id")
    private Boleto boleto; 

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("extraId") 
    @JoinColumn(name = "extra_id")
    private Extra extra; 

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1; 

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario; 
}