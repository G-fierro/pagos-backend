package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.banduairlines.model.enums.Sentido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boletos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasajero_id", nullable = false)
    private Pasajero pasajeroId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vueloId; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_vuelo_id", nullable = false)
    private TarifaVuelo tarifaVueloId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id")
    private Asiento asientoId; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Sentido sentido = Sentido.IDA; 

    @Column(name = "orden_tramo", nullable = false)
    @Builder.Default
    private Integer ordenTramo = 1; 

    @Column(name = "precio_tarifa", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTarifa; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "boleto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoletoExtra> extras = new ArrayList<>();

    public void addExtra(Extra extra, Integer cantidad, BigDecimal precioUnitario) {
        BoletoExtra boletoExtra = BoletoExtra.builder()
                .boleto(this)
                .extra(extra)
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .build();
        this.extras.add(boletoExtra);
    }
}