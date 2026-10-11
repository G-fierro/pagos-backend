package com.banduairlines.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            log.warn("SEGURIDAD DESACTIVADA (app.security.enabled=false): todas las rutas están abiertas. "
                    + "Solo para desarrollo, hasta que CUENTA entregue la validación JWT.");
        }

        http
                .cors(Customizer.withDefaults())                       // usa el bean CorsConfigurationSource
                .csrf(csrf -> csrf.disable())                          // API stateless con JWT: no hay sesión ni cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401 en JSON
                        .accessDeniedHandler(accessDeniedHandler))            // 403 en JSON
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();      // preflight de CORS
                    auth.requestMatchers(SecurityRoutes.PUBLICAS).permitAll();
                    auth.requestMatchers(HttpMethod.GET, SecurityRoutes.PUBLICAS_GET).permitAll();
                    if (securityEnabled) {
                        auth.requestMatchers(SecurityRoutes.SOLO_ADMIN).hasRole("ADMINISTRADOR");
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();                                 // solo desarrollo
                    }
                })
                // Punto de inserción del JWT: CUENTA completa el cuerpo de JwtAuthenticationFilter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Disponible para CUENTA (registro/login). No contiene lógica de negocio. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
