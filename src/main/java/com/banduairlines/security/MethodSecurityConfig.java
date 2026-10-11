package com.banduairlines.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Activa la seguridad por métodos (@PreAuthorize) solo si la seguridad global está habilitada. */
@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class MethodSecurityConfig {
}
