package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarjetas_embarque", uniqueConstraints = {
    @UniqueConstraint(name = "tarjetas_embarque_codigo_embarque_unique", columnNames = {"codigo_embarque"}) 
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaEmbarque {

    @Id
    private Long boletoId; 

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "boleto_id")
    private Boleto boleto; 

    @Column(name = "codigo_embarque", nullable = false, length = 12)
    private String codigoEmbarque; 

    @Column(name = "puerta_embarque", length = 10)
    private String puertaEmbarque; 

    @CreationTimestamp
    @Column(name = "emitida_en", nullable = false, updatable = false)
    private LocalDateTime emitidaEn; 
}