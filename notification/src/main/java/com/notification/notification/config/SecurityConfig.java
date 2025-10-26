package com.notification.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the notification service.
 * Implements authentication and authorization controls with proper security measures.
 *
 * Features:
 * - JWT token-based authentication (when configured)
 * - Stateless session management
 * - Public access to health endpoints
 * - Secured API endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security with authentication and authorization rules.
     *
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF protection - disabled for stateless APIs, enable if using session-based auth
            .csrf(csrf -> csrf.disable())

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()

                // API documentation endpoints (if using Swagger/OpenAPI)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Session management - stateless for REST APIs
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // HTTP Basic authentication for now (replace with OAuth2/JWT in production)
            .httpBasic(basic -> {})

            // Optional: OAuth2 Resource Server configuration (uncomment when ready)
            // .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))

            .build();
    }
}
