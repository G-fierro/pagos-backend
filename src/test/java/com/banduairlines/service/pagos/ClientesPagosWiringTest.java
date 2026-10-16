package com.banduairlines.service.pagos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 4 no registra un RestClient.Builder con las dependencias actuales:
 * los clientes de Flow y Reservas deben poder crearse sin depender de ese bean.
 */
class ClientesPagosWiringTest {

    @Test
    void clientesSeCreanSinBeansGlobales() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(FlowClientImpl.class, ReservasNotificationClient.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(FlowClientImpl.class);
                    assertThat(ctx).hasSingleBean(ReservasNotificationClient.class);
                });
    }
}
