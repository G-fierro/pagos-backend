package com.banduairlines.service.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;


class CompraTokenServiceTest {

    private final CompraTokenService service = new CompraTokenService("secreto-de-prueba");

    @Test
    void esValido_aceptaSoloElTokenDeEsaReserva() {
        String token = service.generar(7L);

        assertThat(service.esValido(7L, token)).isTrue();
        assertThat(service.esValido(8L, token)).isFalse();
        assertThat(service.esValido(7L, "otro")).isFalse();
        assertThat(service.esValido(7L, null)).isFalse();
    }

    @Test
    void generar_dependeDelSecreto() {
        assertThat(new CompraTokenService("otro-secreto").generar(7L)).isNotEqualTo(service.generar(7L));
    }
}
