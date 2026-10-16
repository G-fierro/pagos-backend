package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;

import com.banduairlines.model.enums.Sentido;

import java.math.BigDecimal;

/**
 * Tramo elegido al iniciar la compra (POST /reservas), con la tarifa y su precio congelado.
 * Existe antes que los pasajeros: PUT /reservas/{id}/pasajeros crea un Boleto por pasajero y tramo a partir de aquí.
 */
@Entity
@Table(name = "tramos_reserva", uniqueConstraints = {
    @UniqueConstraint(name = "tramos_reserva_reserva_id_vuelo_id_unique", columnNames = {"reserva_id", "vuelo_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_vuelo_id", nullable = false)
    private TarifaVuelo tarifaVuelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Sentido sentido = Sentido.IDA;

    @Column(nullable = false)
    private Integer orden;

    // Precio por pasajero al momento de crear la reserva.
    @Column(name = "precio_tarifa", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTarifa;
}
