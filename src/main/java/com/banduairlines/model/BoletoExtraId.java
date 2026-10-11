package com.banduairlines.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoletoExtraId implements Serializable {

    @Column(name = "boleto_id")
    private Long boletoId; 

    @Column(name = "extra_id")
    private Long extraId; 
}