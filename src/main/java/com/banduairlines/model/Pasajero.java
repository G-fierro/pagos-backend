package com.banduairlines.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

import com.banduairlines.model.enums.TipoDocumento;
import com.banduairlines.model.enums.TipoPasajero;

@Entity
@Table(name = "pasajeros", uniqueConstraints = {
    @UniqueConstraint(name = "pasajeros_reserva_id_numero_documento_unique", columnNames = {"reserva_id", "numero_documento"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pasajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPasajero tipo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido; 

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento; 

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento; 

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adulto_responsable_id")
    private Pasajero adultoResponsable; 
}